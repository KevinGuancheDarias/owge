package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.rule.timespecial.ActiveTimeSpecialRuleFinderService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveHideUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = HiddenUnitBo.class
)
@MockBean({
        TaggableCacheManager.class,
        ActiveTimeSpecialRuleFinderService.class
})
class HiddenUnitBoTest {
    private final HiddenUnitBo hiddenUnitBo;
    private final TaggableCacheManager taggableCacheManager;
    private final ActiveTimeSpecialRuleFinderService activeTimeSpecialRuleFinderService;

    @Autowired
    public HiddenUnitBoTest(
            HiddenUnitBo hiddenUnitBo,
            TaggableCacheManager taggableCacheManager,
            ActiveTimeSpecialRuleFinderService activeTimeSpecialRuleFinderService) {
        this.hiddenUnitBo = hiddenUnitBo;
        this.activeTimeSpecialRuleFinderService = activeTimeSpecialRuleFinderService;
        this.taggableCacheManager = taggableCacheManager;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void defineHidden_should_work(boolean expectation) {
        var obtainedUnit = givenObtainedUnit1();
        var ouDto = new ObtainedUnitDto();
        ouDto.setUnit(new UnitDto());

        given(activeTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination(
                obtainedUnit.getUser(), obtainedUnit.getUnit(), TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID)
        ).willReturn(expectation);

        hiddenUnitBo.defineHidden(List.of(obtainedUnit), List.of(ouDto));

        assertThat(ouDto.getUnit().getIsInvisible()).isEqualTo(expectation);
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
    }

    @Test
    void isHiddenUnit_should_return_true_when_unit_is_hidden_by_itself() {
        var ou = givenObtainedUnit1();
        ou.getUnit().setIsInvisible(true);

        assertThat(hiddenUnitBo.isHiddenUnit(ou.getUser(), ou.getUnit())).isTrue();
    }
}
