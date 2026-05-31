package com.kevinguanchedarias.owgejava.business.mysql;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.repository.MysqlInformationRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.ThreadUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.kevinguanchedarias.owgejava.business.mysql.MysqlLockUtilService.TIMEOUT_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        classes = MysqlLockUtilService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        JdbcTemplate.class,
        TransactionUtilService.class,
        MysqlInformationRepository.class
})
class MysqlLockUtilServiceTest {
    private static final String KEY_1 = "foo_key";
    private static final String KEY_2 = "bar_key";
    private static final Set<String> KEY_LIST = new LinkedHashSet<>();

    static {
        KEY_LIST.add(KEY_1);
        KEY_LIST.add(KEY_2);
    }

    private static final String EXPECTED_SQL_FOR_LOCK = "SELECT CONCAT(GET_LOCK(?,?),',',GET_LOCK(?,?));";
    private static final String EXPECTED_SQL_FOR_RELEASE_LOCK = "SELECT CONCAT(RELEASE_LOCK(?),',',RELEASE_LOCK(?));";

    private final MysqlLockUtilService mysqlLockUtilService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionUtilService transactionUtilService;
    private final MysqlInformationRepository mysqlInformationRepository;

    private Runnable runnableMock;

    @Autowired
    public MysqlLockUtilServiceTest(
            MysqlLockUtilService mysqlLockUtilService,
            JdbcTemplate jdbcTemplate,
            TransactionUtilService transactionUtilService,
            MysqlInformationRepository mysqlInformationRepository
    ) {
        this.mysqlLockUtilService = mysqlLockUtilService;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionUtilService = transactionUtilService;
        this.mysqlInformationRepository = mysqlInformationRepository;
    }

    @BeforeEach
    public void setup() {
        runnableMock = mock(Runnable.class);
    }

    @AfterEach
    public void clear() {
        // Ensure no corruption of test
        MysqlLockState.clear();
    }

    @Test
    void doInsideLock_should_lock_nothing_if_empty_keys() {
        mysqlLockUtilService.doInsideLock(Set.of(), runnableMock);

        verify(runnableMock, times(1)).run();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void doInsideLock_should_lock_nothing_if_keys_already_locked() {
        try (var mockedStatic = mockStatic(MysqlLockState.class)) {
            mockedStatic.when(MysqlLockState::get).thenReturn(KEY_LIST);
            mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock);

            mockedStatic.verify(() -> MysqlLockState.addAll(any()), never());
        }

        verify(runnableMock, times(1)).run();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void doInsideLock_should_work() throws SQLException {
        var preparedStatementMockForLock = handlePreparedStatementForLock();
        var resultSetMock = mock(ResultSet.class);
        var preparedStatementMockForReleaseLock = handlePreparedStatementForReleaseLock();
        given(preparedStatementMockForLock.executeQuery()).willReturn(resultSetMock);
        given(preparedStatementMockForReleaseLock.executeQuery()).willReturn(resultSetMock);

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

    @Test
    void doInsideLock_should_reacquire_full_union_when_thread_has_other_locks() throws SQLException {
        var alreadyLockedKey = "SOME_ADDITIONAL_KEY";
        // The whole union (already held + new) is re-acquired in one sorted GET_LOCK, so the global
        // acquisition order is always ascending regardless of what an outer frame already held.
        // Sorted order of {SOME_ADDITIONAL_KEY, bar_key, foo_key} -> 'S' < 'b' < 'f'.
        var expectedUnionLockSql = "SELECT CONCAT(GET_LOCK(?,?),',',GET_LOCK(?,?),',',GET_LOCK(?,?));";
        var expectedReleaseHeldSql = "SELECT CONCAT(RELEASE_LOCK(?));";
        var preparedStatementMockForLock = mock(PreparedStatement.class);
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(1, PreparedStatementCallback.class)
                    .doInPreparedStatement(preparedStatementMockForLock);
            return "1,1,1";
        }).when(jdbcTemplate).execute(eq(expectedUnionLockSql), any(PreparedStatementCallback.class));
        given(preparedStatementMockForLock.executeQuery()).willReturn(mock(ResultSet.class));

        try (var mockedStatic = mockStatic(MysqlLockState.class)) {
            mockedStatic.when(MysqlLockState::get).thenReturn(Set.of(alreadyLockedKey));
            mysqlLockUtilService.doInsideLock(Set.of(KEY_1, KEY_2, alreadyLockedKey), runnableMock);

            var captor = ArgumentCaptor.forClass(List.class);
            mockedStatic.verify(() -> MysqlLockState.addAll(captor.capture()), times(1));
            // The full union is (re)acquired, in sorted order.
            assertThat(captor.getValue()).containsExactly(alreadyLockedKey, KEY_2, KEY_1);
        }

        // The previously held key is released first, then the full sorted union is acquired.
        verify(jdbcTemplate, times(1)).execute(eq(expectedReleaseHeldSql), any(PreparedStatementCallback.class));
        verify(jdbcTemplate, times(1)).execute(eq(expectedUnionLockSql), any(PreparedStatementCallback.class));
        verify(preparedStatementMockForLock, times(1)).setString(1, alreadyLockedKey);
        verify(preparedStatementMockForLock, times(1)).setString(3, KEY_2);
        verify(preparedStatementMockForLock, times(1)).setString(5, KEY_1);
        verify(runnableMock, times(1)).run();
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
        given(jdbcTemplate.execute(anyString(), any(PreparedStatementCallback.class))).willReturn("1");
        doThrow(exception).when(runnableMock).run();
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCompletion(any());
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(isActualTransaction);

            assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(Set.of(KEY_1), runnableMock))
                    .isEqualTo(exception);

            verify(jdbcTemplate, times(1))
                    .execute(eq("SELECT CONCAT(GET_LOCK(?,?));"), any(PreparedStatementCallback.class));
            verify(runnableMock, times(1)).run();
            verify(transactionUtilService, times(timesDoAfterCommit)).doAfterCompletion(any());
            verify(jdbcTemplate, times(1))
                    .execute(eq("SELECT CONCAT(RELEASE_LOCK(?));"), any(PreparedStatementCallback.class));
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
    void doInsideLock_should_retry_if_deadlock(CapturedOutput capturedOutput) throws SQLException {
        given(jdbcTemplate.execute(eq(EXPECTED_SQL_FOR_LOCK), any(PreparedStatementCallback.class)))
                .willReturn("1,0")
                .willReturn("1,1");

        try (var unused = mockStatic(ThreadUtil.class)) {
            mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock);
        }

        assertThat(capturedOutput.getOut()).contains("Not able to obtain all required locks");
        verify(jdbcTemplate, times(2)).execute(eq(EXPECTED_SQL_FOR_LOCK), any(PreparedStatementCallback.class));
        verify(runnableMock, times(1)).run();
    }

    @Test
    void doInsideLock_should_throw_if_too_many_deadlocks() {
        given(jdbcTemplate.execute(eq(EXPECTED_SQL_FOR_LOCK), any(PreparedStatementCallback.class)))
                .willReturn("1,0");

        try (var unused = mockStatic(ThreadUtil.class)) {
            // Surrender must fail loudly instead of running the action unprotected, so a @Retryable
            // entry point can retry the whole transaction (and any other caller fails visibly).
            assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock))
                    .isInstanceOf(CannotAcquireLockException.class);
        }

        verify(mysqlInformationRepository, times(1)).findInnoDbStatus();
        verify(mysqlInformationRepository, times(1)).findFullProcessInformation();
        verify(jdbcTemplate, times(5)).execute(eq(EXPECTED_SQL_FOR_LOCK), any(PreparedStatementCallback.class));
        verify(runnableMock, never()).run();
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

    private PreparedStatement handlePreparedStatementForLock() {
        var preparedStatementMockForLock = mock(PreparedStatement.class);
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(1, PreparedStatementCallback.class)
                    .doInPreparedStatement(preparedStatementMockForLock);
            return "1,1";
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
