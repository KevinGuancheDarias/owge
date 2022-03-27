package com.kevinguanchedarias.owgejava.business.rule.type.timespecial;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TimeSpecialIsActiveTemporalUnitsTypeProviderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class TimeSpecialIsActiveTemporalUnitsTypeProviderBoTest {
    private final TimeSpecialIsActiveTemporalUnitsTypeProviderBo timeSpecialIsActiveTemporalUnitsTypeProviderBo;

    @Autowired
    public TimeSpecialIsActiveTemporalUnitsTypeProviderBoTest(TimeSpecialIsActiveTemporalUnitsTypeProviderBo timeSpecialIsActiveTemporalUnitsTypeProviderBo) {
        this.timeSpecialIsActiveTemporalUnitsTypeProviderBo = timeSpecialIsActiveTemporalUnitsTypeProviderBo;
    }

    @Test
    void getRuleTypeId_should_work() {
        assertThat(timeSpecialIsActiveTemporalUnitsTypeProviderBo.getRuleTypeId())
                .isEqualTo(TimeSpecialIsActiveTemporalUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID);
    }

    @Test
    void findRuleTypeDescriptor_should_work() {
        var result = timeSpecialIsActiveTemporalUnitsTypeProviderBo.findRuleTypeDescriptor();

        assertThat(result).isNotNull();
        assertThat(result.getExtraArgs()).isNull();
    }
}
