package com.kevinguanchedarias.owgejava.business.rule.timespecial;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ActiveTimeSpecialMock.givenActiveTimeSpecialMock;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = ActiveTimeSpecialRuleFinderService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ActiveTimeSpecialRepository.class,
        RuleBo.class
})
class ActiveTimeSpecialRuleFinderServiceTest {
    private static final String RULE_TYPE = "FOO_TYPE";
    private final ActiveTimeSpecialRuleFinderService activeTimeSpecialRuleFinderService;
    private final ActiveTimeSpecialRepository repository;
    private final RuleBo ruleBo;

    @Autowired
    ActiveTimeSpecialRuleFinderServiceTest(
            ActiveTimeSpecialRuleFinderService activeTimeSpecialRuleFinderService, ActiveTimeSpecialRepository repository, RuleBo ruleBo
    ) {
        this.activeTimeSpecialRuleFinderService = activeTimeSpecialRuleFinderService;
        this.repository = repository;
        this.ruleBo = ruleBo;
    }

    @ParameterizedTest
    @CsvSource({
            "1,true",
            "0,false"
    })
    void findActiveRules_should_work(short times, boolean isWantedType) {
        var ruleDto = mockFindActiveRules(isWantedType);

        assertThat(activeTimeSpecialRuleFinderService.findActiveRules(givenUser1(), RULE_TYPE)).hasSize(times);

        verify(repository, times(1)).findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE);
        verify(ruleBo, times(1)).findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID);
        verify(ruleBo, times(1)).isWantedType(ruleDto, RULE_TYPE);
    }

    @ParameterizedTest
    @CsvSource({
            "0,false,false,false",
            "1,true,false,false",
            "1,true,true,true"
    })
    void existsRuleMatchingUnitDestination_should_work(
            short times, boolean isWantedType, boolean isWantedUnitDestination, boolean expectation
    ) {
        var ruleDto = mockFindActiveRules(isWantedType);
        var ou = givenObtainedUnit1();
        var unit = ou.getUnit();
        var user = ou.getUser();
        given(ruleBo.isWantedUnitDestination(ruleDto, unit)).willReturn(isWantedUnitDestination);

        assertThat(activeTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination(user, unit, RULE_TYPE)).isEqualTo(expectation);

        verify(ruleBo, times(times)).isWantedUnitDestination(ruleDto, unit);
    }

    private RuleDto mockFindActiveRules(boolean isWantedType) {
        var activeTimeSpecial = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var ruleDto = buildRuleDto(ObjectEnum.UNIT.name());

        given(repository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE))
                .willReturn(List.of(activeTimeSpecial));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(List.of(ruleDto));
        given(ruleBo.isWantedType(ruleDto, RULE_TYPE)).willReturn(isWantedType);
        given(ruleBo.isWantedUnitDestination(any(), any())).willReturn(true);
        return ruleDto;
    }

    private RuleDto buildRuleDto(String destinationType) {
        return RuleDto.builder()
                .type(RULE_TYPE)
                .destinationType(destinationType)
                .build();
    }
}
