package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.rule.timespecial.ActiveTimeSpecialRuleFinderService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = AttackBypassShieldService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(ActiveTimeSpecialRuleFinderService.class)
class AttackBypassShieldServiceTest {
    private final AttackBypassShieldService attackBypassShieldService;
    private final ActiveTimeSpecialRuleFinderService activeTimeSpecialRuleFinderService;

    @Autowired
    AttackBypassShieldServiceTest(
            AttackBypassShieldService attackBypassShieldService, ActiveTimeSpecialRuleFinderService activeTimeSpecialRuleFinderService
    ) {
        this.attackBypassShieldService = attackBypassShieldService;
        this.activeTimeSpecialRuleFinderService = activeTimeSpecialRuleFinderService;
    }

    @ParameterizedTest
    @CsvSource({
            "true,0,false,true",
            "true,0,true,true",
            "false,1,false,false",
            "false,1,true,true"
    })
    void bypassShields_should_work(
            boolean unitBypassShieldByItself, short timesIsRuleMatchingUnitDestination, boolean isRuleMatchingUnitDestination, boolean expectation
    ) {
        var source = givenObtainedUnit1();
        var sourceUser = source.getUser();
        source.getUnit().setBypassShield(unitBypassShieldByItself);
        var target = givenObtainedUnit2();
        var targetUnit = target.getUnit();
        given(activeTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination(
                sourceUser, targetUnit, AttackBypassShieldService.RULE_TYPE
        )).willReturn(isRuleMatchingUnitDestination);

        assertThat(attackBypassShieldService.bypassShields(source, target)).isEqualTo(expectation);

        verify(activeTimeSpecialRuleFinderService, times(timesIsRuleMatchingUnitDestination)).existsRuleMatchingUnitDestination(
                sourceUser, targetUnit, AttackBypassShieldService.RULE_TYPE
        );
    }
}
