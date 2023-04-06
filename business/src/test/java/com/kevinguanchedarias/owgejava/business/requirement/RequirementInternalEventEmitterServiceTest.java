package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.business.requirement.listener.RequirementComplianceListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenUnlockedRelation;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = RequirementInternalEventEmitterService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(RequirementComplianceListener.class)
class RequirementInternalEventEmitterServiceTest {
    private final RequirementInternalEventEmitterService requirementInternalEventEmitterService;
    private final RequirementComplianceListener requirementComplianceListener;

    @Autowired
    RequirementInternalEventEmitterServiceTest(
            RequirementInternalEventEmitterService requirementInternalEventEmitterService,
            RequirementComplianceListener requirementComplianceListener
    ) {
        this.requirementInternalEventEmitterService = requirementInternalEventEmitterService;
        this.requirementComplianceListener = requirementComplianceListener;
    }

    @Test
    void doNotifyObtainedRelation_should_work() {
        var ur = givenUnlockedRelation();

        requirementInternalEventEmitterService.doNotifyObtainedRelation(ur);

        verify(requirementComplianceListener, times(1)).relationObtained(ur);
    }

    @Test
    void doNotifyLostRelation_should_work() {
        var ur = givenUnlockedRelation();

        requirementInternalEventEmitterService.doNotifyLostRelation(ur);

        verify(requirementComplianceListener, times(1)).relationLost(ur);
    }
}
