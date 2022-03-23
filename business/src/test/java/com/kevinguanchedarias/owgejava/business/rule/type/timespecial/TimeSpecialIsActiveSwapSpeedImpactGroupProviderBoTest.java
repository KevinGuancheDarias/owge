package com.kevinguanchedarias.owgejava.business.rule.type.timespecial;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TimeSpecialIsActiveSwapSpeedImpactGroupProviderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class TimeSpecialIsActiveSwapSpeedImpactGroupProviderBoTest {
    private final TimeSpecialIsActiveSwapSpeedImpactGroupProviderBo timeSpecialIsActiveSwapSpeedImpactGroupProviderBo;

    @Autowired
    public TimeSpecialIsActiveSwapSpeedImpactGroupProviderBoTest(TimeSpecialIsActiveSwapSpeedImpactGroupProviderBo timeSpecialIsActiveSwapSpeedImpactGroupProviderBo) {
        this.timeSpecialIsActiveSwapSpeedImpactGroupProviderBo = timeSpecialIsActiveSwapSpeedImpactGroupProviderBo;
    }

    @Test
    void getRuleTypeId_should_work() {
        assertThat(timeSpecialIsActiveSwapSpeedImpactGroupProviderBo.getRuleTypeId())
                .isEqualTo(TimeSpecialIsActiveSwapSpeedImpactGroupProviderBo.TIME_SPECIAL_IS_ACTIVE_SWAP_SPEED_IMPACT_GROUP_ID);
    }

    @Test
    void findRuleTypeDescriptor_should_work() {
        var result = timeSpecialIsActiveSwapSpeedImpactGroupProviderBo.findRuleTypeDescriptor();

        assertThat(result).isNotNull();
        assertThat(result.getExtraArgs()).isNull();
    }
}
