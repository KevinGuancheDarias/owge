package com.kevinguanchedarias.owgejava.business.rule;

import com.kevinguanchedarias.owgejava.business.rule.itemtype.UnitRuleItemTypeProviderBo;
import com.kevinguanchedarias.owgejava.mock.UnitMock;
import com.kevinguanchedarias.owgejava.mock.UnitTypeMock;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.business.rule.type.unit.UnitCaptureRuleTypeProviderBo.PROVIDER_ID;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRule;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = UnitRuleFinderService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(RuleRepository.class)
class UnitRuleFinderServiceTest {
    private static final long UNIT_TYPE_ID = UnitTypeMock.UNIT_TYPE_ID;
    private static final long UNIT_ID = UnitMock.UNIT_ID_1;
    private static final long UNIT_ID_2 = UnitMock.UNIT_ID_2;
    private static final String RULE_DESTINATION_UNIT = "UNIT";
    private static final String RULE_DESTINATION_UNIT_TYPE = "UNIT_TYPE";

    private final UnitRuleFinderService unitRuleFinderService;
    private final RuleRepository ruleRepository;

    @Autowired
    UnitRuleFinderServiceTest(UnitRuleFinderService unitRuleFinderService, RuleRepository ruleRepository) {
        this.unitRuleFinderService = unitRuleFinderService;
        this.ruleRepository = ruleRepository;
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
