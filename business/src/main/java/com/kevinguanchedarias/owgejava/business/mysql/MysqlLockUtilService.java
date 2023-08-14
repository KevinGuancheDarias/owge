package com.kevinguanchedarias.owgejava.business.mysql;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.repository.MysqlInformationRepository;
import com.kevinguanchedarias.owgejava.util.ThreadUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
            var commandLambda = (PreparedStatementCallback<Object>) ps -> {
                generateBindParams(keysAsList, ps);
                return null;
            };
            var releaseLockLambda = (PreparedStatementCallback<Object>) ps -> {
                generateBindParamsWithoutTimeout(keysAsList, ps);
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
        tryAndRetryIfDeadlock(keys, preparedStatement, 0);
    }

    private void generateBindParamsWithoutTimeout(List<String> keys, PreparedStatement preparedStatement) {
        var i = new AtomicInteger(1);
        keys.forEach(key -> {
            try {
                preparedStatement.setString(i.getAndIncrement(), key);
            } catch (SQLException e) {
                throw new IllegalArgumentException("Invalid param for db query", e);
            }
        });
        tryAndRetryIfDeadlock(keys, preparedStatement, 0);
    }

    @SneakyThrows
    private void tryAndRetryIfDeadlock(List<String> keys, PreparedStatement preparedStatement, int tries) {
        try {
            preparedStatement.execute();
        } catch (SQLException e) {
            if (e.getMessage().contains("Deadlock")) {
                if (tries > 4) {
                    log.error("Couldn't solve deadlock for keys {}", keys, e);
                    throw new CommonException("Unhandled deadlock", e);
                } else {
                    log.warn(
                            "Deadlock, retrying lock of ids {}, info: {}, process: {}",
                            keys,
                            mysqlInformationRepository.findInnoDbStatus(),
                            mysqlInformationRepository.findFullProcessInformation()
                    );
                    ThreadUtil.sleep(RandomUtils.nextInt(100, 300));
                    tryAndRetryIfDeadlock(keys, preparedStatement, tries + 1);
                }
            } else {
                throw new CommonException("Unexpected db error", e);
            }
        }
    }
}
