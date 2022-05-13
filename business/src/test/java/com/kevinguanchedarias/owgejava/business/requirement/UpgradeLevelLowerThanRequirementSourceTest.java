package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.owgejava.repository.UpgradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUpgradeMock.givenObtainedUpgrade;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirementInformation;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.UPGRADE_ID;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.givenUpgrade;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UpgradeLevelLowerThanRequirementSource.class
)
@MockBean({
        UpgradeRepository.class,
        ObtainedUpgradeRepository.class
})
class UpgradeLevelLowerThanRequirementSourceTest {
    private final UpgradeLevelLowerThanRequirementSource upgradeLevelLowerThanRequirementSource;
    private final UpgradeRepository upgradeRepository;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;

    @Autowired
    public UpgradeLevelLowerThanRequirementSourceTest(UpgradeLevelLowerThanRequirementSource upgradeLevelLowerThanRequirementSource, UpgradeRepository upgradeRepository, ObtainedUpgradeRepository obtainedUpgradeRepository) {
        this.upgradeLevelLowerThanRequirementSource = upgradeLevelLowerThanRequirementSource;
        this.upgradeRepository = upgradeRepository;
        this.obtainedUpgradeRepository = obtainedUpgradeRepository;
    }

    @Test
    void supports_should_work() {
        assertThat(upgradeLevelLowerThanRequirementSource.supports(RequirementTypeEnum.UPGRADE_LEVEL_LOWER_THAN.name())).isTrue();
        assertThat(upgradeLevelLowerThanRequirementSource.supports("foo")).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "2,4,false,false",
            "2,4,true,true",
            "4,2,true,false"
    })
    void checkRequirementIsMet_should_work(int currentLevel, long requirementLevel, boolean obtainedUpgradeExists, boolean expectation) {
        var upgrade = givenUpgrade();
        var ou = givenObtainedUpgrade();
        ou.setLevel(currentLevel);
        var user = givenUser1();
        var requirementInformation = givenRequirementInformation(
                UPGRADE_ID, requirementLevel, RequirementTypeEnum.UPGRADE_LEVEL_LOWER_THAN
        );
        given(upgradeRepository.findById(UPGRADE_ID)).willReturn(Optional.of(upgrade));
        if (obtainedUpgradeExists) {
            given(obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(USER_ID_1, UPGRADE_ID)).willReturn(ou);
        }

        assertThat(upgradeLevelLowerThanRequirementSource.checkRequirementIsMet(requirementInformation, user)).isEqualTo(expectation);
    }

    @Test
    void checkRequirementIsMet_should_log_warn_when_missing_upgrade(CapturedOutput capturedOutput) {
        assertThat(upgradeLevelLowerThanRequirementSource.checkRequirementIsMet(mock(RequirementInformation.class), givenUser1()))
                .isFalse();
        assertThat(capturedOutput.getOut()).contains("doesn't exists for requirements");
    }
}
