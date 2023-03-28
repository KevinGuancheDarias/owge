package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.business.rule.UnitRuleFinderService;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.business.rule.type.unit.UnitStoresUnitRuleTypeProviderBo.PROVIDER_ID;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRule;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MissionRegistrationCanStoreUnitChecker.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UnitRuleFinderService.class,
        UnitRepository.class
})
class MissionRegistrationCanStoreUnitCheckerTest {
    private final MissionRegistrationCanStoreUnitChecker missionRegistrationCanStoreUnitChecker;
    private final UnitRuleFinderService unitRuleFinderService;
    private final UnitRepository unitRepository;

    @Autowired
    MissionRegistrationCanStoreUnitCheckerTest(
            MissionRegistrationCanStoreUnitChecker missionRegistrationCanStoreUnitChecker,
            UnitRuleFinderService unitRuleFinderService,
            UnitRepository unitRepository
    ) {
        this.missionRegistrationCanStoreUnitChecker = missionRegistrationCanStoreUnitChecker;
        this.unitRuleFinderService = unitRuleFinderService;
        this.unitRepository = unitRepository;
    }

    @Test
    void checkCanStoreUnit_should_work_if_rule_is_present() {
        var unit1 = givenUnit1();
        var unit2 = givenUnit2();
        given(unitRepository.findById(UNIT_ID_1)).willReturn(Optional.of(unit1));
        given(unitRepository.findById(UNIT_ID_2)).willReturn(Optional.of(unit2));

        given(unitRuleFinderService.findRule(PROVIDER_ID, unit1, unit2)).willReturn(Optional.of(givenRule()));

        assertThatCode(() -> missionRegistrationCanStoreUnitChecker.checkCanStoreUnit(UNIT_ID_1, UNIT_ID_2))
                .doesNotThrowAnyException();
    }

    @Test
    void checkCanStoreUnit_should_throw_if_rule_is_not_present() {
        var unit1 = givenUnit1();
        var unit2 = givenUnit2();

        assertThatThrownBy(() -> missionRegistrationCanStoreUnitChecker.checkCanStoreUnit(unit1, unit2))
                .isInstanceOf(SgtBackendInvalidInputException.class);

        verify(unitRuleFinderService, times(1)).findRule(PROVIDER_ID, unit1, unit2);
    }
}
