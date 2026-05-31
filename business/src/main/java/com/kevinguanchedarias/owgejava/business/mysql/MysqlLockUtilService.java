package com.kevinguanchedarias.owgejava.business.mysql;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.repository.MysqlInformationRepository;
import com.kevinguanchedarias.owgejava.util.ThreadUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.dao.CannotAcquireLockException;

@Service
@AllArgsConstructor
@Slf4j
public class MysqlLockUtilService {
    public static final int TIMEOUT_SECONDS = 10;
    public static final int MAX_LOCK_ATTEMPTS = 5;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionUtilService transactionUtilService;
    private final MysqlInformationRepository mysqlInformationRepository;

    public void doInsideLock(Set<String> wantedKeys, Runnable runnable) {
        var alreadyLockedSet = MysqlLockState.get();
        var newKeys = wantedKeys.stream()
                .filter(wantedKey -> !alreadyLockedSet.contains(wantedKey))
                .collect(Collectors.toSet());
        if (newKeys.isEmpty()) {
            log.debug("Not locking as already locked, wanted to lock = {}, already thread-locked = {}", wantedKeys, alreadyLockedSet);
            runnable.run();
            return;
        }
        // GET_LOCK waits in statement order, so two sessions are deadlock-free only if they request
        // the keys they share in the SAME order. Sorting just the new keys is not enough: a key held
        // from an outer frame may be greater than a new key, which would make this session wait for a
        // small key while still holding a bigger one -- exactly the inversion that lets two sessions
        // form a user-level lock deadlock. We therefore release whatever this thread currently holds
        // and re-take the WHOLE union in ascending order, so the global acquisition order is always
        // sorted regardless of nesting.
        //
        // Note: when the caller has pre-acquired every key it will ever need (see
        // UnitMissionBo#runUnitMission), the fast-path above is taken and this re-acquisition --
        // together with the brief window in which the previously held keys are released -- never runs.
        var previouslyHeld = alreadyLockedSet.stream().sorted().toList();
        var fullUnion = Stream.concat(alreadyLockedSet.stream(), wantedKeys.stream())
                .distinct()
                .sorted()
                .toList();
        var newKeysAsList = newKeys.stream().sorted().toList();
        log.trace("Applying the following locks {} (new = {}, already held = {})", fullUnion, newKeysAsList, previouslyHeld);
        var commandLambda = (PreparedStatementCallback<String>) ps -> {
            generateBindParams(fullUnion, ps);
            var rs = ps.executeQuery();
            rs.next();
            return rs.getString(1);
        };

        if (!previouslyHeld.isEmpty()) {
            doReleaseLock(previouslyHeld);
        }
        try {
            tryGainLock(fullUnion, commandLambda, runnable, 1);
        } finally {
            // This frame only owns the keys it introduced; the previously held keys (re-acquired as
            // part of the union) stay owned by the outer frame that first took them.
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                log.debug("Mysql lock was invoked with an active transaction");
                transactionUtilService.doAfterCompletion(() -> doReleaseLock(newKeysAsList));
            } else {
                doReleaseLock(newKeysAsList);
            }
        }
    }

    private void tryGainLock(
            List<String> keysAsList, PreparedStatementCallback<String> preparedStatementCallback, Runnable action, int times
    ) {
        String result = doLock(keysAsList, preparedStatementCallback);
        if (result == null) {
            // A deadlock was detected (see doLock). Retry from scratch, but honour the retry budget
            // so a persistently contended set can no longer recurse without bound.
            doReleaseLock(keysAsList);
            if (times < MAX_LOCK_ATTEMPTS) {
                tryGainLock(keysAsList, preparedStatementCallback, action, times + 1);
            } else {
                surrender(keysAsList, "deadlock");
            }
        } else {
            int acquiredLocks = Arrays.stream(result.split(",")).mapToInt(Integer::valueOf).reduce(0, Integer::sum);
            if (acquiredLocks == keysAsList.size()) {
                MysqlLockState.addAll(keysAsList);
                action.run();
            } else if (times < MAX_LOCK_ATTEMPTS) {
                ThreadUtil.sleep(200);
                log.warn("Not able to obtain all required locks keys = {}, GET_LOCK results = {}", keysAsList, result);
                doReleaseLock(keysAsList);
                tryGainLock(keysAsList, preparedStatementCallback, action, times + 1);
            } else {
                doReleaseLock(keysAsList);
                surrender(keysAsList, "last GET_LOCK result = " + result);
            }
        }
    }

    /**
     * Fails loudly instead of running the protected action without the lock. Previously the service
     * logged and then let execution continue unprotected, which risked silent state corruption (and
     * happened hundreds of times in production logs). Throwing {@link CannotAcquireLockException}
     * lets {@code @Retryable} entry points (e.g. {@code UnitMissionBo#runUnitMission}) retry the whole
     * transaction, and makes any non-retryable caller fail visibly rather than corrupt game state.
     */
    private void surrender(List<String> keysAsList, String reason) {
        log.error(
                "Not able to obtain locks, surrender ({}), wanted keys = {}, info: {}, process: {}",
                reason,
                keysAsList,
                mysqlInformationRepository.findInnoDbStatus(),
                mysqlInformationRepository.findFullProcessInformation()
        );
        throw new CannotAcquireLockException(
                "Could not acquire required MySQL user-level locks after " + MAX_LOCK_ATTEMPTS
                        + " attempts (" + reason + "), keys = " + keysAsList
        );
    }

    private String doLock(List<String> keysAsList, PreparedStatementCallback<String> preparedStatementCallback) {
        try {
            return jdbcTemplate.execute(generateSql("GET_LOCK(?,?)", keysAsList), preparedStatementCallback);
        } catch (Exception e) {
            if (e.getMessage().contains("Deadlock")) {
                log.debug("Handling deadlock");
                return null;
            } else {
                throw e;
            }
        }
    }

    private PreparedStatementCallback<String> releaseLockLambda(List<String> keysAsList) {
        return ps -> {
            generateBindParamsForReleaseLock(keysAsList, ps);
            var rs = ps.executeQuery();
            rs.next();
            return rs.getString(1);
        };
    }

    private String generateSql(String part, List<String> keys) {
        var lastKey = keys.getLast();
        return keys.stream()
                .reduce("SELECT CONCAT(", (buffer, currentKey) -> buffer + part + (currentKey.equals(lastKey) ? ");" : ",',',"));
    }

    private void doReleaseLock(List<String> keysAsList) {
        jdbcTemplate.execute(
                generateSql("RELEASE_LOCK(?)", keysAsList),
                releaseLockLambda(keysAsList)
        );
        MysqlLockState.removeAll(keysAsList);
        var stillLockedKeys = MysqlLockState.get();
        log.trace("Released locks {}", keysAsList);
        if (!stillLockedKeys.isEmpty()) {
            log.debug("While keys {} has been deleted, the thread still contains {}", keysAsList, stillLockedKeys);
        }
    }

    private void generateBindParams(List<String> keys, PreparedStatement preparedStatement) {
        var i = new AtomicInteger(1);
        keys.forEach(key -> {
            try {
                preparedStatement.setString(i.getAndIncrement(), key);
                preparedStatement.setInt(i.getAndIncrement(), TIMEOUT_SECONDS);
            } catch (SQLException e) {
                throw new IllegalArgumentException("Invalid param for db query", e);
            }
        });
    }

    private void generateBindParamsForReleaseLock(List<String> keys, PreparedStatement preparedStatement) {
        var i = new AtomicInteger(1);
        keys.forEach(key -> {
            try {
                preparedStatement.setString(i.getAndIncrement(), key);
            } catch (SQLException e) {
                throw new IllegalArgumentException("Invalid param for db query", e);
            }
        });
    }

}
