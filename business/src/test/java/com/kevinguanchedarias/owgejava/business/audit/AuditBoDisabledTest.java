package com.kevinguanchedarias.owgejava.business.audit;

import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.TorClientBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.repository.AuditRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * When OWGE_AUDIT_ENABLED is false (the default) the recording entry points must be complete no-ops:
 * no audit row is written and the multi-account suspicion scan never runs.
 */
@SpringBootTest(
        classes = AuditBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        AuditRepository.class,
        UserSessionService.class,
        TorClientBo.class,
        AsyncRunnerBo.class,
        SocketIoService.class,
        UserStorageRepository.class,
        AuditMultiAccountSuspicionsService.class
})
class AuditBoDisabledTest {
    private final AuditBo auditBo;
    private final AuditRepository repository;
    private final AuditMultiAccountSuspicionsService auditMultiAccountSuspicionsService;

    @Autowired
    AuditBoDisabledTest(
            AuditBo auditBo,
            AuditRepository repository,
            AuditMultiAccountSuspicionsService auditMultiAccountSuspicionsService
    ) {
        this.auditBo = auditBo;
        this.repository = repository;
        this.auditMultiAccountSuspicionsService = auditMultiAccountSuspicionsService;
    }

    @Test
    void doAudit_should_do_nothing_when_disabled() {
        auditBo.doAudit(AuditActionEnum.REGISTER_MISSION);

        verifyNoInteractions(repository, auditMultiAccountSuspicionsService);
    }

    @Test
    void nonRequestAudit_should_do_nothing_when_disabled() {
        auditBo.nonRequestAudit(AuditActionEnum.REGISTER_MISSION, "foo", givenUser1(), null);

        verifyNoInteractions(repository, auditMultiAccountSuspicionsService);
    }
}
