package com.kevinguanchedarias.owgejava.business.rule;

import com.kevinguanchedarias.owgejava.business.rule.itemtype.UnitRuleItemTypeProviderBo;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.mock.UnitMock;
import com.kevinguanchedarias.owgejava.mock.UnitTypeMock;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.business.rule.type.unit.UnitCaptureRuleTypeProviderBo.PROVIDER_ID;
import static com.kevinguanchedarias.owgejava.mock.ActiveTimeSpecialMock.givenActiveTimeSpecialMock;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRule;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = UnitRuleFinderService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        RuleRepository.class,
        ActiveTimeSpecialRepository.class,
        TaggableCacheManager.class
})
class UnitRuleFinderServiceTest {
    private static final long UNIT_TYPE_ID = UnitTypeMock.UNIT_TYPE_ID;
    private static final long UNIT_ID = UnitMock.UNIT_ID_1;
    private static final long UNIT_ID_2 = UnitMock.UNIT_ID_2;
    private static final String RULE_DESTINATION_UNIT = "UNIT";
    private static final String RULE_DESTINATION_UNIT_TYPE = "UNIT_TYPE";

    private final UnitRuleFinderService unitRuleFinderService;
    private final RuleRepository ruleRepository;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    UnitRuleFinderServiceTest(
            UnitRuleFinderService unitRuleFinderService,
            RuleRepository ruleRepository,
            ActiveTimeSpecialRepository activeTimeSpecialRepository,
            TaggableCacheManager taggableCacheManager
    ) {
        this.unitRuleFinderService = unitRuleFinderService;
        this.ruleRepository = ruleRepository;
        this.activeTimeSpecialRepository = activeTimeSpecialRepository;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Test
    void findRule_should_return_unitVsUnit_rule() {
        var fromOu = givenUnit1();
        var toOu = givenUnit2();

        mockUnitVsUnit();
        mockUnitVsUnitType();
        mockUnitTypeVsUnit();
        mockUnitTypeVsUnitType();

        assertThat(unitRuleFinderService.findRule(PROVIDER_ID, fromOu, toOu)).isPresent().contains(givenRule());

        verifyUsages(0, 0, 0);
    }

    @Test
    void findRule_should_return_unitVsUnitType_rule() {
        var fromOu = givenUnit1();
        var toOu = givenUnit2();

        mockUnitVsUnitType();
        mockUnitTypeVsUnit();
        mockUnitTypeVsUnitType();

        assertThat(unitRuleFinderService.findRule(PROVIDER_ID, fromOu, toOu)).isPresent().contains(givenRule());

        verifyUsages(1, 0, 0);
    }

    @Test
    void findRule_should_return_unitTypeVsUnit_rule() {
        var fromOu = givenUnit1();
        var toOu = givenUnit2();

        mockUnitTypeVsUnit();
        mockUnitTypeVsUnitType();

        assertThat(unitRuleFinderService.findRule(PROVIDER_ID, fromOu, toOu)).isPresent().contains(givenRule());

        verifyUsages(1, 1, 0);
    }

    @Test
    void findRule_should_return_unitTypeVsUnitType_rule() {
        var fromOu = givenUnit1();
        var toOu = givenUnit2();

        mockUnitTypeVsUnitType();

        assertThat(unitRuleFinderService.findRule(PROVIDER_ID, fromOu, toOu)).isPresent().contains(givenRule());

        verifyUsages(1, 1, 1);
    }

    @Test
    void findRule_should_return_empty_if_no_match() {
        var fromOu = givenUnit1();
        var toOu = givenUnit2();

        assertThat(unitRuleFinderService.findRule(PROVIDER_ID, fromOu, toOu)).isEmpty();
        verifyUsages(1, 1, 1);
    }

    @ParameterizedTest
    @CsvSource({
            "UNIT," + UNIT_ID_1,
            "UNIT_TYPE," + UNIT_TYPE_ID
    })
    void findRuleByActiveTimeSpecialsAndTargetUnit_should_work_with_target_unit_rule(String destinationType, long destinationId) {
        var ruleType = "FOO";
        var ats = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var user = ats.getUser();
        var supplier = new InvokeSupplierLambdaAnswer<Optional<Rule>>(2);
        var targetUnit = givenUnit1();
        var rule = givenRule();
        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE)).willReturn(List.of(ats));
        given(ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                ruleType, ObjectEnum.TIME_SPECIAL.name(), (long) TIME_SPECIAL_ID, destinationType, destinationId
        )).willReturn(Optional.of(rule));
        doAnswer(supplier).when(taggableCacheManager).computeIfAbsent(any(), anyList(), any());

        var result = unitRuleFinderService.findRuleByActiveTimeSpecialsAndTargetUnit(ruleType, user, targetUnit);

        assertThat(result).contains(rule);
    }

    @Test
    void findRuleByActiveTimeSpecialsAndTargetUnit_should_work_with_target_unit_type_parent_rule() {
        var ruleType = "FOO";
        var ats = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var user = ats.getUser();
        var supplier = new InvokeSupplierLambdaAnswer<Optional<Rule>>(2);
        var targetUnit = givenUnit1();
        var rule = givenRule();
        var parentUnitTypeId = 1983744;
        var parentUnitType = UnitType.builder()
                .id(parentUnitTypeId)
                .name("FOO_PARENT")
                .build();
        targetUnit.getType().setParent(parentUnitType);

        given(activeTimeSpecialRepository.findByUserIdAndState(USER_ID_1, TimeSpecialStateEnum.ACTIVE)).willReturn(List.of(ats));
        given(ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                ruleType, ObjectEnum.TIME_SPECIAL.name(), (long) TIME_SPECIAL_ID, "UNIT_TYPE", (long) parentUnitTypeId
        )).willReturn(Optional.of(rule));
        doAnswer(supplier).when(taggableCacheManager).computeIfAbsent(any(), anyList(), any());

        var result = unitRuleFinderService.findRuleByActiveTimeSpecialsAndTargetUnit(ruleType, user, targetUnit);

        assertThat(result).contains(rule);
    }

    private void mockUnitVsUnit() {
        when(this.ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                PROVIDER_ID,
                UnitRuleItemTypeProviderBo.PROVIDER_ID,
                UNIT_ID,
                UnitRuleItemTypeProviderBo.PROVIDER_ID,
                UNIT_ID_2
        )).thenReturn(Optional.of(givenRule()));
    }

    private void mockUnitVsUnitType() {
        when(this.ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                PROVIDER_ID,
                UnitRuleItemTypeProviderBo.PROVIDER_ID,
                UNIT_ID,
                UnitRuleFinderService.UNIT_TYPE,
                UNIT_TYPE_ID
        )).thenReturn(Optional.of(givenRule()));
    }

    private void mockUnitTypeVsUnit() {
        when(this.ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                PROVIDER_ID,
                UnitRuleFinderService.UNIT_TYPE,
                UNIT_TYPE_ID,
                UnitRuleItemTypeProviderBo.PROVIDER_ID,
                UNIT_ID_2
        )).thenReturn(Optional.of(givenRule()));
    }

    private void mockUnitTypeVsUnitType() {
        when(this.ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                PROVIDER_ID,
                UnitRuleFinderService.UNIT_TYPE,
                UNIT_TYPE_ID,
                UnitRuleFinderService.UNIT_TYPE,
                UNIT_TYPE_ID
        )).thenReturn(Optional.of(givenRule()));
    }

    private void verifyUsages(int unitVsUnitType, int unitTypeVsUnit, int unitTypeVsUnitType) {
        verify(ruleRepository, times(1)).findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                PROVIDER_ID,
                RULE_DESTINATION_UNIT,
                UNIT_ID,
                RULE_DESTINATION_UNIT,
                UNIT_ID_2
        );
        verify(ruleRepository, times(unitVsUnitType)).findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                PROVIDER_ID,
                RULE_DESTINATION_UNIT,
                UNIT_ID,
                RULE_DESTINATION_UNIT_TYPE,
                UNIT_TYPE_ID
        );
        verify(ruleRepository, times(unitTypeVsUnit)).findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                PROVIDER_ID,
                RULE_DESTINATION_UNIT_TYPE,
                UNIT_TYPE_ID,
                RULE_DESTINATION_UNIT,
                UNIT_ID_2
        );
        verify(ruleRepository, times(unitTypeVsUnitType)).findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                PROVIDER_ID,
                RULE_DESTINATION_UNIT_TYPE,
                UNIT_TYPE_ID,
                RULE_DESTINATION_UNIT_TYPE,
                UNIT_TYPE_ID
        );
    }
}
