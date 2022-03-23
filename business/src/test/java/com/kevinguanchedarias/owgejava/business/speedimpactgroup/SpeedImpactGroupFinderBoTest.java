package com.kevinguanchedarias.owgejava.business.speedimpactgroup;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.unit.util.UnitTypeInheritanceFinderService;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveSwapSpeedImpactGroupProviderBo.TIME_SPECIAL_IS_ACTIVE_SWAP_SPEED_IMPACT_GROUP_ID;
import static com.kevinguanchedarias.owgejava.mock.ActiveTimeSpecialMock.givenActiveTimeSpecialMock;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleDto;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.SPEED_IMPACT_GROUP_ID;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = SpeedImpactGroupFinderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ActiveTimeSpecialRepository.class,
        RuleBo.class,
        SpeedImpactGroupRepository.class,
        UnitTypeInheritanceFinderService.class
})
class SpeedImpactGroupFinderBoTest {
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final RuleBo ruleBo;
    private final SpeedImpactGroupRepository speedImpactGroupRepository;
    private final UnitTypeInheritanceFinderService unitTypeInheritanceFinderService;

    @Autowired
    public SpeedImpactGroupFinderBoTest(
            SpeedImpactGroupFinderBo speedImpactGroupFinderBo,
            ActiveTimeSpecialRepository activeTimeSpecialRepository,
            RuleBo ruleBo,
            SpeedImpactGroupRepository speedImpactGroupRepository,
            UnitTypeInheritanceFinderService unitTypeInheritanceFinderService
    ) {
        this.speedImpactGroupFinderBo = speedImpactGroupFinderBo;
        this.activeTimeSpecialRepository = activeTimeSpecialRepository;
        this.ruleBo = ruleBo;
        this.speedImpactGroupRepository = speedImpactGroupRepository;
        this.unitTypeInheritanceFinderService = unitTypeInheritanceFinderService;
    }

    @Test
    void findHisOrInherited_should_use_units_own_when_not_null() {
        var unit = givenUnit1();
        var sig = givenSpeedImpactGroup();
        unit.setSpeedImpactGroup(sig);

        assertThat(speedImpactGroupFinderBo.findHisOrInherited(unit)).isEqualTo(sig);
        verify(unitTypeInheritanceFinderService, never()).findUnitTypeMatchingCondition(any(UnitType.class), any());
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void findHisOrInherited_should_search_unit_type_parents(boolean expectation) {
        var unit = givenUnit1();
        var sig = givenSpeedImpactGroup();
        if (expectation) {
            unit.getType().setSpeedImpactGroup(sig);
        }
        given(unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(eq(unit.getType()), any()))
                .willReturn(Optional.of(unit.getType()));

        var result = speedImpactGroupFinderBo.findHisOrInherited(unit);
        assertThat(result).isEqualTo(expectation ? sig : null);
        var captor = ArgumentCaptor.forClass(Predicate.class);
        verify(unitTypeInheritanceFinderService, times(1)).findUnitTypeMatchingCondition(eq(unit.getType()), captor.capture());
        assertThat(captor.getValue().test(unit.getType())).isEqualTo(expectation);
    }

    @Test
    void findApplicable_should_search_if_rule_exists_and_apply_or_else_return_unit_one() {
        var ats = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var ruleDto = givenRuleDto().toBuilder().extraArgs(List.of(String.valueOf(SPEED_IMPACT_GROUP_ID))).build();
        var sig = givenSpeedImpactGroup();
        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE)).willReturn(List.of(ats));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID)).willReturn(List.of(ruleDto));
        given(ruleBo.isWantedType(ruleDto, TIME_SPECIAL_IS_ACTIVE_SWAP_SPEED_IMPACT_GROUP_ID)).willReturn(true);
        given(speedImpactGroupRepository.findById(SPEED_IMPACT_GROUP_ID)).willReturn(Optional.of(sig));

        var result = this.speedImpactGroupFinderBo.findApplicable(givenUser1(), givenUnit1());

        assertThat(result).isEqualTo(sig);
        verify(unitTypeInheritanceFinderService, never()).findUnitTypeMatchingCondition(any(UnitType.class), any());
    }

    @ParameterizedTest
    @CsvSource({
            "true,true,true,true",
            "true,true,true,false",
            "true,true,false,false",
            "true,false,false,false"
    })
    void findApplicable_should_use_unit_if_no_rules(boolean atsDefined, boolean rulesNotEmpty, boolean isWantedType, boolean hasExtraArg) {
        var unit = givenUnit1();
        var ats = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var ruleDto = hasExtraArg
                ? givenRuleDto().toBuilder().extraArgs(List.of(String.valueOf(SPEED_IMPACT_GROUP_ID))).build()
                : givenRuleDto().toBuilder().extraArgs(List.of()).build();
        var sig = givenSpeedImpactGroup();
        unit.setSpeedImpactGroup(sig);
        if (atsDefined) {
            given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE)).willReturn(List.of(ats));
        }
        if (rulesNotEmpty) {
            given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID)).willReturn(List.of(ruleDto));
        }
        if (hasExtraArg) {
            given(speedImpactGroupRepository.findById(SPEED_IMPACT_GROUP_ID)).willReturn(Optional.of(sig));
        }
        given(ruleBo.isWantedType(ruleDto, TIME_SPECIAL_IS_ACTIVE_SWAP_SPEED_IMPACT_GROUP_ID)).willReturn(isWantedType);

        var result = speedImpactGroupFinderBo.findApplicable(givenUser1(), unit);

        assertThat(result).isEqualTo(sig);
        verify(speedImpactGroupRepository, times(hasExtraArg ? 1 : 0)).findById(SPEED_IMPACT_GROUP_ID);
    }
}
