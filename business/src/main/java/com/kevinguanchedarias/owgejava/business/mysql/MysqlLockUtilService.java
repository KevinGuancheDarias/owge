package com.kevinguanchedarias.owgejava.business.mysql;

import com.kevinguanchedarias.owgejava.exception.CommonException;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@AllArgsConstructor
public class MysqlLockUtilService {
    public static final int TIMEOUT_SECONDS = 10;
    private final JdbcTemplate jdbcTemplate;

    public void doInsideLock(Set<String> keys, Runnable runnable) {
        var keysAsList = keys.stream().toList();
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
            jdbcTemplate.execute(
                    generateSql("RELEASE_LOCK(?)", keysAsList),
                    releaseLockLambda
            );
        }
    }

    private String generateSql(String part, List<String> keys) {
        var lastKey = keys.get(keys.size() - 1);
        return keys.stream()
                .reduce("", (buffer, result) -> buffer + "SELECT " + part + (result.equals(lastKey) ? ";" : " UNION "));
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
        try {
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new CommonException("Unexpected db error", e);
        }
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
        try {
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new CommonException("Unexpected db error", e);
        }
    }
}
