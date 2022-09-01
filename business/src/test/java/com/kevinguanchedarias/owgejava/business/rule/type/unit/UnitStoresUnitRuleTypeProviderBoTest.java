package com.kevinguanchedarias.owgejava.business.rule.type.unit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitStoresUnitRuleTypeProviderBo.class
)
class UnitStoresUnitRuleTypeProviderBoTest {
    private final UnitStoresUnitRuleTypeProviderBo unitStoresUnitRuleTypeProviderBo;

    @Autowired
    UnitStoresUnitRuleTypeProviderBoTest(UnitStoresUnitRuleTypeProviderBo unitStoresUnitRuleTypeProviderBo) {
        this.unitStoresUnitRuleTypeProviderBo = unitStoresUnitRuleTypeProviderBo;
    }

    @Test
    void getRuleTypeId_should_work() {
        assertThat(unitStoresUnitRuleTypeProviderBo.getRuleTypeId()).isEqualTo("UNIT_STORES_UNIT");
    }

    @Test
    void findRuleTypeDescriptor_should_work() {
        assertThat(unitStoresUnitRuleTypeProviderBo.findRuleTypeDescriptor()).isNotNull();
    }
}
