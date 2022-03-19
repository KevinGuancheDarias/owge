package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.rule.type.TimeSpecialIsActiveHideUnitsTypeProviderBo;
import com.kevinguanchedarias.owgejava.business.unit.util.UnitTypeInheritanceFinderService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokePredicateLambdaAnswer;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.kevinguanchedarias.owgejava.business.ActiveTimeSpecialBo.ACTIVE_TIME_SPECIAL_CACHE_TAG_BY_USER;
import static com.kevinguanchedarias.owgejava.business.UnitBo.UNIT_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.UnitTypeBo.UNIT_TYPE_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.rule.RuleBo.RULE_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.mock.ActiveTimeSpecialMock.givenActiveTimeSpecialMock;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = HiddenUnitBo.class
)
@MockBean({
        ActiveTimeSpecialRepository.class,
        RuleBo.class,
        UnitTypeInheritanceFinderService.class,
        TaggableCacheManager.class
})
class HiddenUnitBoTest {
    private final HiddenUnitBo hiddenUnitBo;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final RuleBo ruleBo;
    private final UnitTypeInheritanceFinderService unitTypeInheritanceFinderService;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public HiddenUnitBoTest(
            HiddenUnitBo hiddenUnitBo,
            ActiveTimeSpecialRepository activeTimeSpecialRepository,
            RuleBo ruleBo,
            UnitTypeInheritanceFinderService unitTypeInheritanceFinderService,
            TaggableCacheManager taggableCacheManager
    ) {
        this.hiddenUnitBo = hiddenUnitBo;
        this.activeTimeSpecialRepository = activeTimeSpecialRepository;
        this.ruleBo = ruleBo;
        this.unitTypeInheritanceFinderService = unitTypeInheritanceFinderService;
        this.taggableCacheManager = taggableCacheManager;
    }

    @ParameterizedTest
    @CsvSource({
            UNIT_ID_1 + ",true",
            "1726617,false"
    })
    void defineHidden_should_work(int ruleDestinationId, boolean expectation) {
        var obtainedUnit = givenObtainedUnit1();
        var ouDto = new ObtainedUnitDto();
        ouDto.setUnit(new UnitDto());
        var activeTimeSpecial = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var ruleDto = buildRuleDto(ObjectEnum.UNIT.name(), ruleDestinationId);

        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE))
                .willReturn(List.of(activeTimeSpecial));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(List.of(ruleDto));

        hiddenUnitBo.defineHidden(List.of(obtainedUnit), List.of(ouDto));

        verify(activeTimeSpecialRepository, times(1)).findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE);
        verify(ruleBo, times(1)).findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID);
        verify(unitTypeInheritanceFinderService, never()).findUnitTypeMatchingCondition(any(), any());
        assertThat(ouDto.getUnit().getIsInvisible()).isEqualTo(expectation);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isHiddenUnit_should_use_cached_value(boolean expectation) {
        var ou = new ObtainedUnit();
        ou.setId(192781L);
        var expectedKey = "com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo_defineHidden() 192840";
        given(taggableCacheManager.keyExists(expectedKey)).willReturn(true);
        given(taggableCacheManager.findByKey(expectedKey)).willReturn(expectation);

        assertThat(hiddenUnitBo.isHiddenUnit(ou)).isEqualTo(expectation);

        verify(taggableCacheManager, times(1)).keyExists(expectedKey);
        verify(taggableCacheManager, times(1)).findByKey(expectedKey);
        verify(activeTimeSpecialRepository, never()).findByUserIdAndState(any(), any());
    }

    @ParameterizedTest
    @CsvSource({
            UNIT_TYPE_ID + ",true",
            "1726617,false"
    })
    void isHiddenUnit_should_return_correct_when_it_is_by_target_unit_type(int ruleDestinationId, boolean expectation) {
        var activeTimeSpecial = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var obtainedUnit = givenObtainedUnit1();
        var unitToCheck = obtainedUnit.getUnit();
        var unitType = unitToCheck.getType();
        var ruleDto = buildRuleDto("UNIT_TYPE", ruleDestinationId);
        AtomicReference<Boolean> lambdaResult = new AtomicReference<>();

        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE))
                .willReturn(List.of(activeTimeSpecial));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(List.of(ruleDto));
        doAnswer(generateAnswer(
                unitType,
                () -> lambdaResult.get() ? Optional.of(unitType) : Optional.empty(),
                lambdaResult::set
        ))
                .when(unitTypeInheritanceFinderService).findUnitTypeMatchingCondition(eq(unitToCheck.getType()), any());

        var result = hiddenUnitBo.isHiddenUnit(obtainedUnit);

        assertThat(result).isEqualTo(expectation);
        verify(activeTimeSpecialRepository, times(1)).findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE);
        verify(ruleBo, times(1)).findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID);
        verify(unitTypeInheritanceFinderService, times(1)).findUnitTypeMatchingCondition(eq(unitType), any());
        verify(taggableCacheManager, times(1)).saveEntry(any(), eq(expectation), eq(List.of(
                RULE_CACHE_TAG,
                ACTIVE_TIME_SPECIAL_CACHE_TAG_BY_USER + ":" + USER_ID_1,
                UNIT_TYPE_CACHE_TAG,
                UNIT_CACHE_TAG
        )));
        assertThat(lambdaResult.get()).isEqualTo(expectation);
    }

    @ParameterizedTest
    @CsvSource({
            UNIT_ID_1 + ",true",
            "1726617,false"
    })
    void isHiddenUnit_should_return_correct_when_it_is_by_target_unit(int ruleDestinationId, boolean expectation) {
        var activeTimeSpecial = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var obtainedUnit = givenObtainedUnit1();
        var ruleDto = buildRuleDto(ObjectEnum.UNIT.name(), ruleDestinationId);

        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE))
                .willReturn(List.of(activeTimeSpecial));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(List.of(ruleDto));

        var result = hiddenUnitBo.isHiddenUnit(obtainedUnit);

        assertThat(result).isEqualTo(expectation);
        verify(activeTimeSpecialRepository, times(1)).findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE);
        verify(ruleBo, times(1)).findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID);
        verify(unitTypeInheritanceFinderService, never()).findUnitTypeMatchingCondition(any(), any());
    }

    @Test
    void isHiddenUnit_should_return_false_when_destination_for_rule_is_not_valid(CapturedOutput capturedOutput) {
        var activeTimeSpecial = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var obtainedUnit = givenObtainedUnit1();
        var ruleDto = buildRuleDto("invalid", 12);

        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE))
                .willReturn(List.of(activeTimeSpecial));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(List.of(ruleDto));

        var result = hiddenUnitBo.isHiddenUnit(obtainedUnit);

        assertThat(result).isFalse();
        verify(activeTimeSpecialRepository, times(1)).findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE);
        verify(ruleBo, times(1)).findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID);
        assertThat(capturedOutput.getOut())
                .contains("is not wanted destination for rule");
    }

    @Test
    void isHiddenUnit_should_return_false_when_no_tiem_special_is_active() {
        assertThat(hiddenUnitBo.isHiddenUnit(givenObtainedUnit1())).isFalse();
    }

    @Test
    void isHiddenUnit_should_return_true_when_unit_is_hidden_by_itself() {
        var ou = givenObtainedUnit1();
        ou.getUnit().setIsInvisible(true);

        assertThat(hiddenUnitBo.isHiddenUnit(ou)).isTrue();
        verify(activeTimeSpecialRepository, never()).findByUserIdAndState(any(), any());
    }

    private RuleDto buildRuleDto(String destinationType, long destinationId) {
        return RuleDto.builder()
                .type(TimeSpecialIsActiveHideUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID)
                .destinationType(destinationType)
                .destinationId(destinationId)
                .build();
    }

    private Answer<Optional<UnitType>> generateAnswer(UnitType unitType, Supplier<Optional<UnitType>> result, Consumer<Boolean> lambdaResult) {
        return invocationOnMock -> {
            var unitTypeFinderPredicate = new InvokePredicateLambdaAnswer<>(1, unitType);
            lambdaResult.accept(unitTypeFinderPredicate.answer(invocationOnMock));
            return result.get();
        };
    }
}
