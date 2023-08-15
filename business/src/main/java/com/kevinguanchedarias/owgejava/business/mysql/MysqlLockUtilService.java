package com.kevinguanchedarias.owgejava.business.mysql;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.repository.MysqlInformationRepository;
import com.kevinguanchedarias.owgejava.util.ThreadUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@AllArgsConstructor
@Slf4j
public class MysqlLockUtilService {
    public static final int TIMEOUT_SECONDS = 10;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionUtilService transactionUtilService;
    private final MysqlInformationRepository mysqlInformationRepository;
    private final Map<String, List<Pair<Integer, List<String>>>> locksInformation = new ConcurrentHashMap<>();

    public void doInsideLock(Set<String> keys, Runnable runnable) {
        if (keys.isEmpty()) {
            runnable.run();
        } else {
            locksInformation.putIfAbsent(Thread.currentThread().getName(), new ArrayList<>());
            var entry = locksInformation.get(Thread.currentThread().getName());
            var keysAsList = keys.stream().sorted().toList();
            var tries = new AtomicInteger();
            var index = entry.size();
            var commandLambda = (PreparedStatementCallback<Object>) ps -> {
                var numTries = generateBindParams(keysAsList, ps);
                entry.add(Pair.of(numTries, keys.stream().toList()));
                tries.set(numTries);
                return null;
            };
            var releaseLockLambda = (PreparedStatementCallback<Object>) ps -> {
                generateBindParamsForReleaseLock(keysAsList, ps, tries.get());
                entry.remove(index);
                return null;
            };

            try {
                jdbcTemplate.execute(generateSql("GET_LOCK(?,?)", keysAsList), commandLambda);
                runnable.run();
            } finally {
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    log.debug("Mysql lock was invoked with an active transaction");
                    transactionUtilService.doAfterCommit(() -> doReleaseLock(keysAsList, releaseLockLambda));
                } else {
                    doReleaseLock(keysAsList, releaseLockLambda);
                }
            }
        }
    }

    private String generateSql(String part, List<String> keys) {
        var lastKey = keys.get(keys.size() - 1);
        return keys.stream()
                .reduce("", (buffer, result) -> buffer + "SELECT " + part + (result.equals(lastKey) ? ";" : " UNION "));
    }

    private void doReleaseLock(List<String> keysAsList, PreparedStatementCallback<Object> releaseLockLambda) {
        jdbcTemplate.execute(
                generateSql("RELEASE_LOCK(?)", keysAsList),
                releaseLockLambda
        );
    }

    private int generateBindParams(List<String> keys, PreparedStatement preparedStatement) {
        var i = new AtomicInteger(1);
        keys.forEach(key -> {
            try {
                preparedStatement.setString(i.getAndIncrement(), key);
                preparedStatement.setInt(i.getAndIncrement(), TIMEOUT_SECONDS);
            } catch (SQLException e) {
                throw new IllegalArgumentException("Invalid param for db query", e);
            }
        });
        return tryAndRetryIfDeadlock(keys, preparedStatement, 0);
    }

    private void generateBindParamsForReleaseLock(List<String> keys, PreparedStatement preparedStatement, int times) {
        var i = new AtomicInteger(1);
        keys.forEach(key -> {
            try {
                preparedStatement.setString(i.getAndIncrement(), key);
            } catch (SQLException e) {
                throw new IllegalArgumentException("Invalid param for db query", e);
            }
        });
        for (int j = 0; j < (times + 2); j++) {
            tryAndRetryIfDeadlock(keys, preparedStatement, 0);
        }
    }

    @SneakyThrows
    private int tryAndRetryIfDeadlock(List<String> keys, PreparedStatement preparedStatement, int tries) {
        try {
            preparedStatement.execute();
            return tries;
        } catch (SQLException e) {
            if (e.getMessage().contains("Deadlock")) {
                if (tries > 4) {
                    log.error("Couldn't solve deadlock for keys {}, sql: {}", keys, preparedStatement);
                    throw new CommonException("Unhandled deadlock", e);
                } else {
                    log.warn(
                            "Deadlock, retrying lock of ids {}, info: {}, process: {}, lock map: {}",
                            keys,
                            mysqlInformationRepository.findInnoDbStatus(),
                            mysqlInformationRepository.findFullProcessInformation(),
                            locksInformation
                    );
                    ThreadUtil.sleep(RandomUtils.nextInt(100, 300));
                    return tryAndRetryIfDeadlock(keys, preparedStatement, tries + 1);
                }
            } else {
                throw new CommonException("Unexpected db error", e);
            }
        }
    }
}
