package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.FactionMock.FACTION_INITIAL_ENERGY;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenFaction;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = UserEnergyServiceBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImprovementBo.class,
        ObtainedUnitRepository.class
})
class UserEnergyServiceBoTest {
    private final UserEnergyServiceBo userEnergyServiceBo;
    private final ImprovementBo improvementBo;
    private final ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    UserEnergyServiceBoTest(
            UserEnergyServiceBo userEnergyServiceBo,
            ImprovementBo improvementBo,
            ObtainedUnitRepository obtainedUnitRepository
    ) {
        this.userEnergyServiceBo = userEnergyServiceBo;
        this.improvementBo = improvementBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
    }

    @ParameterizedTest
    @CsvSource(value = {
            "50,50",
            "null,0"
    }, nullValues = "null")
    void findConsumedEnergy_should_work(Double repositoryResult, double expectedResult) {
        var user = givenUser1();
        given(obtainedUnitRepository.computeConsumedEnergyByUser(user)).willReturn(repositoryResult);

        assertThat(userEnergyServiceBo.findConsumedEnergy(user)).isEqualTo(expectedResult);
    }

    @Test
    void findMaxEnergy_should_work() {
        var user = givenUser1();
        var faction = givenFaction();
        user.setFaction(faction);
        var inputPercentage = 4F;
        var groupedImprovementMock = mock(GroupedImprovement.class);
        var expectedRetVal = 118.74D;
        given(groupedImprovementMock.getMoreEnergyProduction()).willReturn(inputPercentage);
        given(improvementBo.findUserImprovement(user)).willReturn(groupedImprovementMock);
        given(improvementBo.computeImprovementValue(FACTION_INITIAL_ENERGY, inputPercentage)).willReturn(expectedRetVal);

        assertThat(userEnergyServiceBo.findMaxEnergy(user)).isEqualTo(expectedRetVal);
    }

    @Test
    void findAvailableEnergy_should_work() {
        var user = givenUser1();
        var faction = givenFaction();
        user.setFaction(faction);
        var inputPercentage = 4F;
        var groupedImprovementMock = mock(GroupedImprovement.class);
        var maxEnergy = 118.74D;
        var consumedEnergy = 68.74D;
        given(groupedImprovementMock.getMoreEnergyProduction()).willReturn(inputPercentage);
        given(improvementBo.findUserImprovement(user)).willReturn(groupedImprovementMock);
        given(improvementBo.computeImprovementValue(FACTION_INITIAL_ENERGY, inputPercentage)).willReturn(maxEnergy);
        given(obtainedUnitRepository.computeConsumedEnergyByUser(user)).willReturn(consumedEnergy);

        assertThat(userEnergyServiceBo.findAvailableEnergy(user)).isEqualTo(50D);
    }
}
