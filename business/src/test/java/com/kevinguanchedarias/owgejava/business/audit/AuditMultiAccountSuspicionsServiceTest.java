package com.kevinguanchedarias.owgejava.business.audit;

import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.Suspicion;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.SuspicionSourceEnum;
import com.kevinguanchedarias.owgejava.repository.AuditRepository;
import com.kevinguanchedarias.owgejava.repository.SuspicionRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.AuditMock.*;
import static com.kevinguanchedarias.owgejava.mock.SuspicionMock.SUSPICION_ID;
import static com.kevinguanchedarias.owgejava.mock.SuspicionMock.givenSuspicion;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = AuditMultiAccountSuspicionsService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        AsyncRunnerBo.class,
        AuditRepository.class,
        SuspicionRepository.class
})
class AuditMultiAccountSuspicionsServiceTest {
    private final AuditMultiAccountSuspicionsService auditMultiAccountSuspicionsService;
    private final AsyncRunnerBo asyncRunnerBo;
    private final AuditRepository auditRepository;
    private final SuspicionRepository suspicionRepository;

    @Autowired
    AuditMultiAccountSuspicionsServiceTest(
            AuditMultiAccountSuspicionsService auditMultiAccountSuspicionsService,
            AsyncRunnerBo asyncRunnerBo,
            AuditRepository auditRepository,
            SuspicionRepository suspicionRepository
    ) {
        this.auditMultiAccountSuspicionsService = auditMultiAccountSuspicionsService;
        this.asyncRunnerBo = asyncRunnerBo;
        this.auditRepository = auditRepository;
        this.suspicionRepository = suspicionRepository;
    }

    @Test
    void handle_should_work() {
        var user = givenUser2();
        var triggeringAudit = givenAudit().toBuilder().user(user).build();
        var suspicionIpMatchingAudit = givenAudit().toBuilder().id(144L).cookie(null).build();
        var suspicionBrowserMatchingAudit = givenAudit().toBuilder().id(145L).ipv4(null).ipv6(null).build();
        var bothMatchingAudit = givenAudit().toBuilder().id(146L).build();

        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(asyncRunnerBo)
                .runAsyncWithoutContextDelayed(any(), eq(AuditMultiAccountSuspicionsService.WANTED_MS_DELAY), eq(Thread.MIN_PRIORITY));
        given(auditRepository.findSuspicions(any(), eq(user), eq(AUDIT_COOKIE), eq(AUDIT_IP), eq(AUDIT_IPV6))).willReturn(
                List.of(suspicionIpMatchingAudit, suspicionBrowserMatchingAudit, bothMatchingAudit)
        );

        auditMultiAccountSuspicionsService.handle(triggeringAudit);

        var captor = ArgumentCaptor.forClass(Suspicion.class);
        verify(suspicionRepository, times(3)).save(captor.capture());
        var savedValues = captor.getAllValues();
        doAssertSaved(savedValues.get(0), SuspicionSourceEnum.IP, user, suspicionIpMatchingAudit);
        doAssertSaved(savedValues.get(1), SuspicionSourceEnum.BROWSER, user, suspicionBrowserMatchingAudit);
        doAssertSaved(savedValues.get(2), SuspicionSourceEnum.BROWSER_AND_IP, user, bothMatchingAudit);
    }

    @Test
    void handle_should_do_nothing_if_ip_and_cookie_are_both_null() {
        var audit = givenAudit().toBuilder().cookie(null).ipv4(null).ipv6(null).build();

        auditMultiAccountSuspicionsService.handle(audit);

        verifyNoInteractions(asyncRunnerBo, suspicionRepository, auditRepository);

    }

    @Test
    void handle_should_do_nothing_if_audit_was_already_processed() {
        var user = givenUser2();
        var triggeringAudit = givenAudit().toBuilder().id(AUDIT_ID + 1).user(user).build();
        var alreadyProcessedAudit = givenAudit();

        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(asyncRunnerBo)
                .runAsyncWithoutContextDelayed(any(), eq(AuditMultiAccountSuspicionsService.WANTED_MS_DELAY), eq(Thread.MIN_PRIORITY));
        given(auditRepository.findSuspicions(any(), eq(user), eq(AUDIT_COOKIE), eq(AUDIT_IP), eq(AUDIT_IPV6))).willReturn(
                List.of(alreadyProcessedAudit)
        );
        given(suspicionRepository.existsByRelatedUserAndRelatedAudit(user, alreadyProcessedAudit)).willReturn(true);

        auditMultiAccountSuspicionsService.handle(triggeringAudit);

        verify(suspicionRepository, never()).save(any(Suspicion.class));
    }

    @Test
    void findLast100_should_work() {
        try (
                var pageRequestMockedStatic = mockStatic(PageRequest.class);
                var sortMockedStatic = mockStatic(Sort.class)
        ) {
            var pageRequestMock = mock(PageRequest.class);
            var sortMock = mock(Sort.class);
            pageRequestMockedStatic.when(() -> PageRequest.of(0, 100, sortMock))
                    .thenReturn(pageRequestMock);
            sortMockedStatic.when(() -> Sort.by(Sort.Direction.DESC, "createdAt"))
                    .thenReturn(sortMock);
            var suspicion = givenSuspicion();
            given(suspicionRepository.findAll(pageRequestMock)).willReturn(new PageImpl<>(List.of(suspicion)));

            var retVal = auditMultiAccountSuspicionsService.findLast100();

            assertThat(retVal).hasSize(1);
            var entry = retVal.get(0);
            assertThat(entry.id()).isEqualTo(SUSPICION_ID);
        }
    }

    private void doAssertSaved(Suspicion target, SuspicionSourceEnum source, UserStorage user, Audit audit) {
        assertThat(target.getSource()).isEqualTo(source);
        assertThat(target.getRelatedUser()).isEqualTo(user);
        assertThat(target.getRelatedAudit()).isEqualTo(audit);
        assertThat(target.getCreatedAt()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }
}
