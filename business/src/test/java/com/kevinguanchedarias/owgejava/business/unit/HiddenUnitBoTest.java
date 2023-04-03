package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveHideUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID;
import static com.kevinguanchedarias.owgejava.entity.Rule.RULE_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.entity.Unit.UNIT_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.entity.UnitType.UNIT_TYPE_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.mock.ActiveTimeSpecialMock.givenActiveTimeSpecialMock;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = HiddenUnitBo.class
)
@MockBean({
        ActiveTimeSpecialRepository.class,
        RuleBo.class,
        TaggableCacheManager.class
})
class HiddenUnitBoTest {
    private final HiddenUnitBo hiddenUnitBo;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final RuleBo ruleBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public HiddenUnitBoTest(
            HiddenUnitBo hiddenUnitBo,
            ActiveTimeSpecialRepository activeTimeSpecialRepository,
            RuleBo ruleBo,
            TaggableCacheManager taggableCacheManager
    ) {
        this.hiddenUnitBo = hiddenUnitBo;
        this.activeTimeSpecialRepository = activeTimeSpecialRepository;
        this.ruleBo = ruleBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void defineHidden_should_work(boolean expectation) {
        var obtainedUnit = givenObtainedUnit1();
        var ouDto = new ObtainedUnitDto();
        ouDto.setUnit(new UnitDto());
        var activeTimeSpecial = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var ruleDto = buildRuleDto(ObjectEnum.UNIT.name());

        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE))
                .willReturn(List.of(activeTimeSpecial));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(List.of(ruleDto));
        given(ruleBo.isWantedType(ruleDto, TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID)).willReturn(true);
        given(ruleBo.isWantedUnitDestination(ruleDto, obtainedUnit.getUnit()))
                .willReturn(expectation);

        hiddenUnitBo.defineHidden(List.of(obtainedUnit), List.of(ouDto));

        verify(activeTimeSpecialRepository, times(1)).findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE);
        verify(ruleBo, times(1)).findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID);
        assertThat(ouDto.getUnit().getIsInvisible()).isEqualTo(expectation);
    }

    @ParameterizedTest
    @CsvSource({
            "1,true",
            "0,false"
    })
    void defineHidden_should_skip_not_wanted_rules(short times, boolean expectation) {
        var obtainedUnit = givenObtainedUnit1();
        var activeTimeSpecial = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var ruleDto = buildRuleDto(ObjectEnum.UNIT.name());

        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE))
                .willReturn(List.of(activeTimeSpecial));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(List.of(ruleDto));
        given(ruleBo.isWantedType(ruleDto, TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID)).willReturn(expectation);
        given(ruleBo.isWantedUnitDestination(any(), any())).willReturn(true);

        assertThat(hiddenUnitBo.isHiddenUnit(obtainedUnit.getUser(), obtainedUnit.getUnit())).isEqualTo(expectation);

        verify(activeTimeSpecialRepository, times(1)).findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE);
        verify(ruleBo, times(1)).findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID);
        verify(ruleBo, times(1)).isWantedType(ruleDto, TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID);
        verify(ruleBo, times(times)).isWantedUnitDestination(ruleDto, obtainedUnit.getUnit());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isHiddenUnit_should_use_cached_value(boolean expectation) {
        var ou = new ObtainedUnit();
        ou.setId(192781L);
        ou.setUser(givenUser1());
        ou.setUnit(givenUnit1());
        var expectedKey = "com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo_defineHidden() " + USER_ID_1 + "_" + UNIT_ID_1;
        given(taggableCacheManager.keyExists(expectedKey)).willReturn(true);
        given(taggableCacheManager.findByKey(expectedKey)).willReturn(expectation);

        assertThat(hiddenUnitBo.isHiddenUnit(ou.getUser(), ou.getUnit())).isEqualTo(expectation);

        verify(taggableCacheManager, times(1)).keyExists(expectedKey);
        verify(taggableCacheManager, times(1)).findByKey(expectedKey);
        verify(activeTimeSpecialRepository, never()).findByUserIdAndState(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isHiddenUnit_should_return_correct_when_is_wanted_destination(boolean expectation) {
        var activeTimeSpecial = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var obtainedUnit = givenObtainedUnit1();
        var unitToCheck = obtainedUnit.getUnit();
        var ruleDto = buildRuleDto("UNIT_TYPE");

        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE))
                .willReturn(List.of(activeTimeSpecial));
        given(ruleBo.findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(List.of(ruleDto));
        given(ruleBo.isWantedUnitDestination(ruleDto, unitToCheck)).willReturn(expectation);
        given(ruleBo.isWantedType(ruleDto, TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID)).willReturn(true);

        var result = hiddenUnitBo.isHiddenUnit(obtainedUnit.getUser(), obtainedUnit.getUnit());

        assertThat(result).isEqualTo(expectation);
        verify(activeTimeSpecialRepository, times(1)).findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE);
        verify(ruleBo, times(1)).findByOriginTypeAndOriginId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID);
        verify(taggableCacheManager, times(1)).saveEntry(any(), eq(expectation), eq(List.of(
                RULE_CACHE_TAG,
                ActiveTimeSpecial.ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG + ":" + USER_ID_1,
                UNIT_TYPE_CACHE_TAG,
                UNIT_CACHE_TAG
        )));
    }

    @Test
    void isHiddenUnit_should_return_false_when_no_time_special_is_active() {
        var ou = givenObtainedUnit1();
        assertThat(hiddenUnitBo.isHiddenUnit(ou.getUser(), ou.getUnit())).isFalse();
        verify(ruleBo, never()).isWantedUnitDestination(any(), any());
    }

    @Test
    void isHiddenUnit_should_return_true_when_unit_is_hidden_by_itself() {
        var ou = givenObtainedUnit1();
        ou.getUnit().setIsInvisible(true);

        assertThat(hiddenUnitBo.isHiddenUnit(ou.getUser(), ou.getUnit())).isTrue();
        verify(activeTimeSpecialRepository, never()).findByUserIdAndState(any(), any());
    }

    private RuleDto buildRuleDto(String destinationType) {
        return RuleDto.builder()
                .type(TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID)
                .destinationType(destinationType)
                .build();
    }
}
