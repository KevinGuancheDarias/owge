package com.kevinguanchedarias.owgejava.business.rule.type;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TimeSpecialIsActiveHideUnitsTypeProviderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class TimeSpecialIsActiveHideUnitsTypeProviderBoTest {
    private final TimeSpecialIsActiveHideUnitsTypeProviderBo timeSpecialIsActiveHideUnitsTypeProviderBo;

    @Autowired
    public TimeSpecialIsActiveHideUnitsTypeProviderBoTest(TimeSpecialIsActiveHideUnitsTypeProviderBo timeSpecialIsActiveHideUnitsTypeProviderBo) {
        this.timeSpecialIsActiveHideUnitsTypeProviderBo = timeSpecialIsActiveHideUnitsTypeProviderBo;
    }

    @Test
    void getRuleTypeId_should_work() {
        assertThat(timeSpecialIsActiveHideUnitsTypeProviderBo.getRuleTypeId())
                .isEqualTo(TimeSpecialIsActiveHideUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID);
    }

    @Test
    void findRuleTypeDescriptor_should_work() {
        var result = timeSpecialIsActiveHideUnitsTypeProviderBo.findRuleTypeDescriptor();

        assertThat(result).isNotNull();
        assertThat(result.getExtraArgs()).isNull();
    }
}
