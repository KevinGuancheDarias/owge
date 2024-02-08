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

@Service
@AllArgsConstructor
@Slf4j
public class MysqlLockUtilService {
    public static final int TIMEOUT_SECONDS = 10;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionUtilService transactionUtilService;
    private final MysqlInformationRepository mysqlInformationRepository;

    public void doInsideLock(Set<String> keys, Runnable runnable) {
        if (keys.isEmpty()) {
            runnable.run();
        } else {
            var keysAsList = keys.stream().sorted().toList();
            var commandLambda = (PreparedStatementCallback<String>) ps -> {
                generateBindParams(keysAsList, ps);
                var rs = ps.executeQuery();
                rs.next();
                return rs.getString(1);
            };

            try {
                tryGainLock(keysAsList, commandLambda, runnable, 1);
            } finally {
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    log.debug("Mysql lock was invoked with an active transaction");
                    transactionUtilService.doAfterCompletion(() -> doReleaseLock(keysAsList));
                } else {
                    doReleaseLock(keysAsList);
                }
            }
        }
    }

    private void tryGainLock(
            List<String> keysAsList, PreparedStatementCallback<String> preparedStatementCallback, Runnable action, int times
    ) {
        String result = jdbcTemplate.execute(generateSql("GET_LOCK(?,?)", keysAsList), preparedStatementCallback);
        if (result == null) {
            throw new IllegalStateException("result can't be null");
        } else {
            int acquiredLocks = Arrays.stream(result.split(",")).mapToInt(Integer::valueOf).reduce(0, Integer::sum);
            if (acquiredLocks == keysAsList.size()) {
                action.run();
            } else if (times < 5) {
                ThreadUtil.sleep(200);
                log.warn("Not able to obtain all required locks keys = {}, GET_LOCK results = {}", keysAsList, result);
                doReleaseLock(keysAsList);
                tryGainLock(keysAsList, preparedStatementCallback, action, times + 1);
            } else {
                log.error(
                        "Not able to obtain locks, surrender, wanted keys = {}, last result = {}, info: {}, process: {}",
                        keysAsList,
                        result,
                        mysqlInformationRepository.findInnoDbStatus(),
                        mysqlInformationRepository.findFullProcessInformation()
                );
                doReleaseLock(keysAsList);
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
        var lastKey = keys.get(keys.size() - 1);
        return keys.stream()
                .reduce("SELECT CONCAT(", (buffer, currentKey) -> buffer + part + (currentKey.equals(lastKey) ? ");" : ",',',"));
    }

    private void doReleaseLock(List<String> keysAsList) {
        jdbcTemplate.execute(
                generateSql("RELEASE_LOCK(?)", keysAsList),
                releaseLockLambda(keysAsList)
        );
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
