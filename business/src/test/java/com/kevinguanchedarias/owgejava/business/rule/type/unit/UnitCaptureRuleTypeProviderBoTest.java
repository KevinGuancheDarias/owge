package com.kevinguanchedarias.owgejava.business.rule.type.unit;

import com.kevinguanchedarias.owgejava.dto.rule.RuleExtraArgDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitCaptureRuleTypeProviderBo.class
)
class UnitCaptureRuleTypeProviderBoTest {
    private final UnitCaptureRuleTypeProviderBo unitCaptureRuleTypeProviderBo;

    @Autowired
    public UnitCaptureRuleTypeProviderBoTest(UnitCaptureRuleTypeProviderBo unitCaptureRuleTypeProviderBo) {
        this.unitCaptureRuleTypeProviderBo = unitCaptureRuleTypeProviderBo;
    }

    @Test
    void getRuleTypeId_should_work() {
        assertThat(unitCaptureRuleTypeProviderBo.getRuleTypeId()).isEqualTo("UNIT_CAPTURE");
    }

    @Test
    void findRuleTypeDescriptor_should_work() {
        var expectedExtraArgs = List.of(
                RuleExtraArgDto.builder().id(1).name("CRUD.RULES.ARG.UNIT_CAPTURE_PROBABILITY").formType("number").build()
        );

        var result = unitCaptureRuleTypeProviderBo.findRuleTypeDescriptor();

        assertThat(result).isNotNull();
        assertThat(result.getExtraArgs()).hasSize(2).containsAll(expectedExtraArgs);
    }
}
