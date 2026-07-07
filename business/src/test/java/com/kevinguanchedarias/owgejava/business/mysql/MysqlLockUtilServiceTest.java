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

import static com.kevinguanchedarias.owgejava.business.mysql.MysqlLockUtilService.GET_LOCK_SQL;
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

    private static final String EXPECTED_SQL_FOR_RELEASE_LOCK = "SELECT CONCAT(RELEASE_LOCK(?),',',RELEASE_LOCK(?));";
    private static final String EXPECTED_SQL_FOR_RELEASE_SINGLE_LOCK = "SELECT CONCAT(RELEASE_LOCK(?));";

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
        givenLockAcquired(KEY_1);
        givenLockAcquired(KEY_2);
        var preparedStatementMockForReleaseLock = handlePreparedStatementForReleaseLock();
        given(preparedStatementMockForReleaseLock.executeQuery()).willReturn(mock(ResultSet.class));

        mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock);

        // Keys are acquired one statement at a time, in ascending order, so a session can never
        // hold a key greater than one it's waiting for (would allow deadlock cycles)
        var inOrder = inOrder(jdbcTemplate, runnableMock);
        inOrder.verify(jdbcTemplate, times(1)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_2, TIMEOUT_SECONDS);
        inOrder.verify(jdbcTemplate, times(1)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_1, TIMEOUT_SECONDS);
        inOrder.verify(runnableMock, times(1)).run();
        verify(jdbcTemplate, times(1)).execute(eq(EXPECTED_SQL_FOR_RELEASE_LOCK), any(PreparedStatementCallback.class));
        verify(preparedStatementMockForReleaseLock, times(1)).setString(1, KEY_2);
        verify(preparedStatementMockForReleaseLock, times(1)).setString(2, KEY_1);
    }

    @Test
    void doInsideLock_should_reacquire_full_union_when_thread_has_other_locks() {
        var alreadyLockedKey = "SOME_ADDITIONAL_KEY";
        // The whole union (already held + new) is re-acquired in ascending order, so the global
        // acquisition order stays sorted regardless of what an outer frame already held.
        // Sorted order of {SOME_ADDITIONAL_KEY, bar_key, foo_key} -> 'S' < 'b' < 'f'.
        givenLockAcquired(alreadyLockedKey);
        givenLockAcquired(KEY_1);
        givenLockAcquired(KEY_2);

        try (var mockedStatic = mockStatic(MysqlLockState.class)) {
            mockedStatic.when(MysqlLockState::get).thenReturn(Set.of(alreadyLockedKey));
            mysqlLockUtilService.doInsideLock(Set.of(KEY_1, KEY_2, alreadyLockedKey), runnableMock);

            var captor = ArgumentCaptor.forClass(List.class);
            mockedStatic.verify(() -> MysqlLockState.addAll(captor.capture()), times(1));
            // The full union is (re)acquired, in sorted order.
            assertThat(captor.getValue()).containsExactly(alreadyLockedKey, KEY_2, KEY_1);
        }

        // The previously held key is released first, then the full sorted union is acquired.
        verify(jdbcTemplate, times(1)).execute(eq(EXPECTED_SQL_FOR_RELEASE_SINGLE_LOCK), any(PreparedStatementCallback.class));
        var inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate, times(1)).queryForObject(GET_LOCK_SQL, Integer.class, alreadyLockedKey, TIMEOUT_SECONDS);
        inOrder.verify(jdbcTemplate, times(1)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_2, TIMEOUT_SECONDS);
        inOrder.verify(jdbcTemplate, times(1)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_1, TIMEOUT_SECONDS);
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
        givenLockAcquired(KEY_1);
        doThrow(exception).when(runnableMock).run();
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCompletion(any());
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(isActualTransaction);

            assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(Set.of(KEY_1), runnableMock))
                    .isEqualTo(exception);

            verify(jdbcTemplate, times(1)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_1, TIMEOUT_SECONDS);
            verify(runnableMock, times(1)).run();
            verify(transactionUtilService, times(timesDoAfterCommit)).doAfterCompletion(any());
            verify(jdbcTemplate, times(1))
                    .execute(eq(EXPECTED_SQL_FOR_RELEASE_SINGLE_LOCK), any(PreparedStatementCallback.class));
        }
    }

    @Test
    void doInsideLock_should_propagate_non_deadlock_lock_error() {
        var exception = new RuntimeException("something else broke");
        givenLockAcquired(KEY_2);
        given(jdbcTemplate.queryForObject(GET_LOCK_SQL, Integer.class, KEY_1, TIMEOUT_SECONDS)).willThrow(exception);

        assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock))
                .isEqualTo(exception);

        // The acquired prefix (bar_key) must be released before propagating
        verify(jdbcTemplate, atLeastOnce()).execute(eq(EXPECTED_SQL_FOR_RELEASE_SINGLE_LOCK), any(PreparedStatementCallback.class));
        verify(runnableMock, never()).run();
    }

    @Test
    void doInsideLock_should_release_prefix_and_retry_when_a_key_is_not_acquired(CapturedOutput capturedOutput) {
        givenLockAcquired(KEY_2);
        given(jdbcTemplate.queryForObject(GET_LOCK_SQL, Integer.class, KEY_1, TIMEOUT_SECONDS))
                .willReturn(0)
                .willReturn(1);

        try (var unused = mockStatic(ThreadUtil.class)) {
            mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock);
        }

        assertThat(capturedOutput.getOut()).contains("Not able to obtain lock");
        verify(jdbcTemplate, times(2)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_2, TIMEOUT_SECONDS);
        verify(jdbcTemplate, times(2)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_1, TIMEOUT_SECONDS);
        // The prefix acquired before the failing key (bar_key) is released before the retry, so the
        // session doesn't keep keys while backing off
        verify(jdbcTemplate, times(1)).execute(eq(EXPECTED_SQL_FOR_RELEASE_SINGLE_LOCK), any(PreparedStatementCallback.class));
        verify(runnableMock, times(1)).run();
    }

    @Test
    void doInsideLock_should_handle_deadlock_as_failed_attempt_and_retry(CapturedOutput capturedOutput) {
        given(jdbcTemplate.queryForObject(GET_LOCK_SQL, Integer.class, KEY_2, TIMEOUT_SECONDS))
                .willThrow(new RuntimeException("Deadlock found when trying to get user-level lock"))
                .willReturn(1);
        givenLockAcquired(KEY_1);

        try (var unused = mockStatic(ThreadUtil.class)) {
            mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock);
        }

        assertThat(capturedOutput.getOut()).contains("Not able to obtain lock");
        verify(jdbcTemplate, times(2)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_2, TIMEOUT_SECONDS);
        verify(jdbcTemplate, times(1)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_1, TIMEOUT_SECONDS);
        verify(runnableMock, times(1)).run();
    }

    @Test
    void doInsideLock_should_throw_when_all_attempts_fail() {
        given(jdbcTemplate.queryForObject(GET_LOCK_SQL, Integer.class, KEY_2, TIMEOUT_SECONDS)).willReturn(0);

        try (var unused = mockStatic(ThreadUtil.class)) {
            // Surrender must fail loudly instead of running the action unprotected, so a @Retryable
            // entry point can retry the whole transaction (and any other caller fails visibly).
            assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock))
                    .isInstanceOf(CannotAcquireLockException.class);
        }

        verify(mysqlInformationRepository, times(1)).findInnoDbStatus();
        verify(mysqlInformationRepository, times(1)).findFullProcessInformation();
        verify(jdbcTemplate, times(5)).queryForObject(GET_LOCK_SQL, Integer.class, KEY_2, TIMEOUT_SECONDS);
        // Acquisition stops at the first failed key: the greater key is never even requested
        verify(jdbcTemplate, never()).queryForObject(GET_LOCK_SQL, Integer.class, KEY_1, TIMEOUT_SECONDS);
        verify(runnableMock, never()).run();
    }

    @Test
    void doInsideLock_should_properly_handle_db_error_on_release_lock_arg_binding() throws SQLException {
        givenLockAcquired(KEY_1);
        givenLockAcquired(KEY_2);
        var preparedStatementMock = handlePreparedStatementForReleaseLock();
        var exception = new SQLException("FOO", "BAR");
        doThrow(exception).when(preparedStatementMock).setString(anyInt(), anyString());

        assertThatThrownBy(() -> mysqlLockUtilService.doInsideLock(KEY_LIST, runnableMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasCause(exception);
    }

    private void givenLockAcquired(String key) {
        given(jdbcTemplate.queryForObject(GET_LOCK_SQL, Integer.class, key, TIMEOUT_SECONDS)).willReturn(1);
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
