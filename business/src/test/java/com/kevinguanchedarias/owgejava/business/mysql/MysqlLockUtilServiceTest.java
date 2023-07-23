package com.kevinguanchedarias.owgejava.business.mysql;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.kevinguanchedarias.owgejava.business.mysql.MysqlLockUtilService.TIMEOUT_SECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@SpringBootTest(
        classes = MysqlLockUtilService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        JdbcTemplate.class,
        TransactionUtilService.class
})
class MysqlLockUtilServiceTest {
    private static final String KEY_1 = "foo_key";
    private static final String KEY_2 = "bar_key";
    private static final Set<String> KEY_LIST = new LinkedHashSet<>();

    static {
        KEY_LIST.add(KEY_1);
        KEY_LIST.add(KEY_2);
    }

    private static final String EXPECTED_SQL_FOR_LOCK = "SELECT GET_LOCK(?,?) UNION SELECT GET_LOCK(?,?);";
    private static final String EXPECTED_SQL_FOR_RELEASE_LOCK = "SELECT RELEASE_LOCK(?) UNION SELECT RELEASE_LOCK(?);";

    private final MysqlLockUtilService mysqlLockUtilService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionUtilService transactionUtilService;

    private Runnable runnableMock;

    @Autowired
    public MysqlLockUtilServiceTest(
            MysqlLockUtilService mysqlLockUtilService,
            JdbcTemplate jdbcTemplate,
            TransactionUtilService transactionUtilService
    ) {
        this.mysqlLockUtilService = mysqlLockUtilService;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionUtilService = transactionUtilService;
    }

    @BeforeEach
    public void setup() {
        runnableMock = mock(Runnable.class);
    }

    @Test
    void doInsideLock_should_lock_nothing_if_empty_keys() {
        mysqlLockUtilService.doInsideLock(Set.of(), runnableMock);

        verify(runnableMock, times(1)).run();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void doInsideLock_should_work() throws SQLException {
        var preparedStatementMockForLock = handlePreparedStatementForLock();
        var preparedStatementMockForReleaseLock = handlePreparedStatementForReleaseLock();

        mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock);

        verify(jdbcTemplate, times(1)).execute(eq(EXPECTED_SQL_FOR_LOCK), any(PreparedStatementCallback.class));
        verify(jdbcTemplate, times(1)).execute(eq(EXPECTED_SQL_FOR_RELEASE_LOCK), any(PreparedStatementCallback.class));
        verify(preparedStatementMockForLock, times(1)).setString(1, KEY_2);
        verify(preparedStatementMockForLock, times(1)).setInt(2, TIMEOUT_SECONDS);
        verify(preparedStatementMockForLock, times(1)).setString(3, KEY_1);
        verify(preparedStatementMockForLock, times(1)).setInt(4, TIMEOUT_SECONDS);
        verify(preparedStatementMockForReleaseLock, times(1)).setString(1, KEY_2);
        verify(preparedStatementMockForReleaseLock, times(1)).setString(2, KEY_1);
    }

    @CsvSource({
            "true,1",
            "false,0"
    })
    @ParameterizedTest
    void doInsideLock_should_release_lock_even_if_lambda_throws(
            boolean isActualTransaction,
            int timesDoAfterCommit
    ) {
        var exception = new RuntimeException("foo");
        doThrow(exception).when(runnableMock).run();
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(isActualTransaction);

            assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(Set.of(KEY_1), runnableMock))
                    .isEqualTo(exception);

            verify(jdbcTemplate, times(1))
                    .execute(eq("SELECT GET_LOCK(?,?);"), any(PreparedStatementCallback.class));
            verify(runnableMock, times(1)).run();
            verify(transactionUtilService, times(timesDoAfterCommit)).doAfterCommit(any());
            verify(jdbcTemplate, times(1))
                    .execute(eq("SELECT RELEASE_LOCK(?);"), any(PreparedStatementCallback.class));
        }
    }

    @Test
    void doInsideLock_should_properly_handle_db_error_on_lock_arg_binding() throws SQLException {
        var preparedStatementMockForLock = handlePreparedStatementForLock();
        var exception = new SQLException("FOO", "BAR");
        doThrow(exception).when(preparedStatementMockForLock).setString(anyInt(), anyString());

        assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasCause(exception);
    }

    @Test
    void doInsideLock_should_properly_handle_db_error_on_release_lock_arg_binding() throws SQLException {
        var preparedStatementMock = handlePreparedStatementForReleaseLock();
        var exception = new SQLException("FOO", "BAR");
        doThrow(exception).when(preparedStatementMock).setString(anyInt(), anyString());

        assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasCause(exception);
    }

    @Test
    void doInsideLock_should_properly_handle_db_error_on_lock() throws SQLException {
        var preparedStatementMock = handlePreparedStatementForLock();
        var exception = new SQLException("FOO", "BAR");
        doThrow(exception).when(preparedStatementMock).execute();

        assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock))
                .isInstanceOf(CommonException.class)
                .hasCause(exception);
    }

    @Test
    void doInsideLock_should_properly_handle_db_error_on_release_lock() throws SQLException {
        var preparedStatementMock = handlePreparedStatementForReleaseLock();
        var exception = new SQLException("FOO", "BAR");
        doThrow(exception).when(preparedStatementMock).execute();

        assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock))
                .isInstanceOf(CommonException.class)
                .hasCause(exception);
    }

    private PreparedStatement handlePreparedStatementForLock() {
        var preparedStatementMockForLock = mock(PreparedStatement.class);
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(1, PreparedStatementCallback.class)
                    .doInPreparedStatement(preparedStatementMockForLock);
            return null;
        }).when(jdbcTemplate).execute(eq(EXPECTED_SQL_FOR_LOCK), any(PreparedStatementCallback.class));
        return preparedStatementMockForLock;
    }

    private PreparedStatement handlePreparedStatementForReleaseLock() {
        var preparedStatementMockForReleaseLock = mock(PreparedStatement.class);
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(1, PreparedStatementCallback.class)
                    .doInPreparedStatement(preparedStatementMockForReleaseLock);
            return null;
        }).when(jdbcTemplate).execute(eq(EXPECTED_SQL_FOR_RELEASE_LOCK), any(PreparedStatementCallback.class));
        return preparedStatementMockForReleaseLock;
    }

}
