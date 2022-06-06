package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.requirement.RequirementSource;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.InvalidConfigurationException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructRequirementBo;
import com.kevinguanchedarias.owgejava.mock.ObjectRelationMock;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.BEEN_RACE;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.HAVE_SPECIAL_AVAILABLE;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.HAVE_SPECIAL_ENABLED;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.HAVE_SPECIAL_LOCATION;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.HAVE_UNIT;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.HOME_GALAXY;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.UNIT_AMOUNT;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.UPGRADE_LEVEL;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.UPGRADE_LEVEL_LOWER_THAN;
import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.WORST_PLAYER;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.OBJECT_RELATION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.REFERENCE_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.UNLOCKED_RELATION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectEntity;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenUnlockedRelation;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUpgradeMock.givenObtainedUpgrade;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirement;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirementInformation;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.SECOND_VALUE;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.givenTimeSpecial;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.UPGRADE_ID;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.givenUpgrade;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = NonPostConstructRequirementBo.class
)
@MockBean({
        RequirementRepository.class,
        UserStorageBo.class,
        RequirementInformationDao.class,
        UnlockedRelationBo.class,
        ObtainedUpgradeBo.class,
        UpgradeBo.class,
        ObjectRelationBo.class,
        ObjectRelationToObjectRelationBo.class,
        DtoUtilService.class,
        RequirementInformationBo.class,
        SocketIoService.class,
        TimeSpecialBo.class,
        UnitBo.class,
        SpeedImpactGroupBo.class,
        PlanetBo.class,
        EntityManager.class,
        ObtainedUnitRepository.class,
        UnitRepository.class,
        RequirementSource.class,
        TransactionUtilService.class,
        ObtainedUpgradeRepository.class,
        FactionBo.class,
        SpecialLocationBo.class,
        GalaxyBo.class
})
class RequirementBoTest {
    private final NonPostConstructRequirementBo requirementBo;
    private final RequirementSource requirementSource;
    private final ObjectRelationBo objectRelationBo;
    private final UnlockedRelationBo unlockedRelationBo;
    private final ObtainedUpgradeBo obtainedUpgradeBo;
    private final UpgradeBo upgradeBo;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;
    private final RequirementRepository requirementRepository;
    private final BeanFactory beanFactory;

    @Autowired
    RequirementBoTest(
            NonPostConstructRequirementBo requirementBo,
            RequirementSource requirementSource,
            ObjectRelationBo objectRelationBo,
            UnlockedRelationBo unlockedRelationBo,
            ObtainedUpgradeBo obtainedUpgradeBo,
            UpgradeBo upgradeBo,
            ObtainedUpgradeRepository obtainedUpgradeRepository,
            RequirementRepository requirementRepository,
            DefaultListableBeanFactory beanFactory
    ) {
        this.requirementBo = requirementBo;
        this.requirementSource = requirementSource;
        this.objectRelationBo = objectRelationBo;
        this.unlockedRelationBo = unlockedRelationBo;
        this.obtainedUpgradeBo = obtainedUpgradeBo;
        this.upgradeBo = upgradeBo;
        this.obtainedUpgradeRepository = obtainedUpgradeRepository;
        this.requirementRepository = requirementRepository;
        this.beanFactory = beanFactory;
    }

    @Test
    void init_should_work() {
        given(requirementRepository.findAll()).willReturn(List.of(
                givenRequirement(HAVE_SPECIAL_LOCATION),
                givenRequirement(HAVE_UNIT),
                givenRequirement(BEEN_RACE),
                givenRequirement(UPGRADE_LEVEL),
                givenRequirement(WORST_PLAYER),
                givenRequirement(UNIT_AMOUNT),
                givenRequirement(HOME_GALAXY),
                givenRequirement(HAVE_SPECIAL_AVAILABLE),
                givenRequirement(HAVE_SPECIAL_ENABLED),
                givenRequirement(UPGRADE_LEVEL_LOWER_THAN)
        ));

        assertThatNoException().isThrownBy(requirementBo::realInit);
    }

    @Test
    void init_should_fail() {
        given(requirementRepository.findAll()).willReturn(List.of(
                givenRequirement(HAVE_SPECIAL_LOCATION),
                givenRequirement(HAVE_UNIT),
                givenRequirement(BEEN_RACE)
        ));

        assertThatThrownBy(requirementBo::realInit)
                .isInstanceOf(InvalidConfigurationException.class)
                .hasCauseExactlyInstanceOf(SgtCorruptDatabaseException.class);
    }

    @SuppressWarnings({"rawtypes"})
    @ParameterizedTest
    @MethodSource("findBoByRequirement_arguments")
    void findBoByRequirement_should_work(RequirementTypeEnum enumValue, Class<BaseBo> expectedBo) {
        var result = requirementBo.findBoByRequirement(enumValue);
        assertThat(result).isInstanceOf(expectedBo);
    }

    @Test
    void triggerTimeSpecialActivated_should_work_and_mark_relation_as_unlocked() {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        timeSpecial.setId((int) SECOND_VALUE);
        var or = givenObjectRelation();
        var requirement = givenRequirementInformation(TIME_SPECIAL_ID, RequirementTypeEnum.HAVE_SPECIAL_ENABLED);
        var upgrade = givenUpgrade(REFERENCE_ID);
        or.setRequirements(List.of(requirement));
        given(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE))
                .willReturn(List.of(or));
        given(requirementSource.supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name())).willReturn(true);
        given(requirementSource.checkRequirementIsMet(requirement, user)).willReturn(true);
        given(upgradeBo.findById(upgrade.getId())).willReturn(upgrade);

        requirementBo.triggerTimeSpecialStateChange(user, timeSpecial);

        verify(objectRelationBo, times(1)).findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE);
        verify(requirementSource, times(1)).supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name());
        verify(requirementSource, times(1)).checkRequirementIsMet(requirement, user);
        var captor = ArgumentCaptor.forClass(UnlockedRelation.class);
        verify(unlockedRelationBo, times(1)).save(captor.capture());
        var savedUnlocked = captor.getValue();
        assertThat(savedUnlocked.getRelation()).isEqualTo(or);
        assertThat(savedUnlocked.getUser()).isEqualTo(user);
        checkRegisterObtainedUpgrade(or, upgrade, user);
    }

    @ParameterizedTest
    @CsvSource({
            "true,true",
            "true,false",
            "false,false",
            "false,true"
    })
    void triggerTimeSpecialActivated_should_work_and_mark_relation_as_lost(boolean lostUpgradeRelationExists, boolean isUpgrade) {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        timeSpecial.setId((int) SECOND_VALUE);
        var or = givenObjectRelation();
        if (!isUpgrade) {
            or.setObject(givenObjectEntity(ObjectEnum.UNIT));
        }
        var requirement = givenRequirementInformation(TIME_SPECIAL_ID, RequirementTypeEnum.HAVE_SPECIAL_ENABLED);
        var upgrade = givenUpgrade(REFERENCE_ID);
        var obtainedUpgrade = givenObtainedUpgrade(REFERENCE_ID, user);
        obtainedUpgrade.setAvailable(true);
        var ur = givenUnlockedRelation(user);
        or.setRequirements(List.of(requirement));
        given(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE))
                .willReturn(List.of(or));
        given(requirementSource.supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name())).willReturn(true);
        given(upgradeBo.findById(upgrade.getId())).willReturn(upgrade);
        given(unlockedRelationBo.findOneByUserIdAndRelationId(USER_ID_1, OBJECT_RELATION_ID)).willReturn(ur);
        given(obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(USER_ID_1, REFERENCE_ID)).willReturn(obtainedUpgrade);
        given(obtainedUpgradeRepository.existsByUserIdAndUpgradeId(USER_ID_1, REFERENCE_ID)).willReturn(lostUpgradeRelationExists);

        requirementBo.triggerTimeSpecialStateChange(user, timeSpecial);

        verify(objectRelationBo, times(1)).findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE);
        verify(requirementSource, times(1)).supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name());
        verify(requirementSource, times(1)).checkRequirementIsMet(requirement, user);

        verify(unlockedRelationBo, times(1)).findOneByUserIdAndRelationId(USER_ID_1, OBJECT_RELATION_ID);
        verify(unlockedRelationBo, times(1)).delete(UNLOCKED_RELATION_ID);
        verify(obtainedUpgradeRepository, times(isUpgrade ? 1 : 0)).existsByUserIdAndUpgradeId(USER_ID_1, REFERENCE_ID);
        var captor = ArgumentCaptor.forClass(ObtainedUpgrade.class);
        verify(obtainedUpgradeBo, times(isUpgrade && lostUpgradeRelationExists ? 1 : 0)).save(captor.capture());
        if (lostUpgradeRelationExists && isUpgrade) {
            var savedLostUpgrade = captor.getValue();
            assertThat(savedLostUpgrade).isSameAs(obtainedUpgrade);
            assertThat(savedLostUpgrade.isAvailable()).isFalse();
        }

    }

    @Test
    void triggerTimeSpecialActivated_should_throw_when_or_has_unsupported_requirement() {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        timeSpecial.setId((int) SECOND_VALUE);
        var or = givenObjectRelation();
        var requirement = givenRequirementInformation(TIME_SPECIAL_ID, RequirementTypeEnum.HAVE_SPECIAL_ENABLED);
        or.setRequirements(List.of(requirement));
        given(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE))
                .willReturn(List.of(or));

        assertThatThrownBy(() -> requirementBo.triggerTimeSpecialStateChange(user, timeSpecial))
                .isInstanceOf(SgtBackendNotImplementedException.class)
                .hasMessageContaining("Not implemented requirement type")
                .hasMessageContaining(requirement.getRequirement().getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "true,true",
            "false,false",
            "true,false"
    })
    void triggerLevelUpCompleted_should_work(boolean obtainedUpgradeExists, boolean unlockedRelationExists) {
        var or = givenObjectRelation();
        or.setReferenceId(UPGRADE_ID);
        or.setRequirements(List.of(givenRequirementInformation(UPGRADE_ID, 0, UPGRADE_LEVEL)));
        var obtainedUpgrade = givenObtainedUpgrade();
        given(objectRelationBo.findByRequirementTypeAndSecondValue(UPGRADE_LEVEL, (long) UPGRADE_ID))
                .willReturn(List.of(or));
        var upgrade = obtainedUpgrade.getUpgrade();
        var user = obtainedUpgrade.getUser();
        given(upgradeBo.findById(UPGRADE_ID)).willReturn(upgrade);
        if (obtainedUpgradeExists) {
            given(obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(USER_ID_1, UPGRADE_ID)).willReturn(obtainedUpgrade);
        }
        given(obtainedUpgradeRepository.existsByUserIdAndUpgradeId(USER_ID_1, UPGRADE_ID)).willReturn(obtainedUpgradeExists);
        var ur = ObjectRelationMock.givenUnlockedRelation(user);
        ur.setRelation(or);
        if (unlockedRelationExists) {
            given(unlockedRelationBo.findOneByUserIdAndRelationId(USER_ID_1, OBJECT_RELATION_ID))
                    .willReturn(ur);
        }

        requirementBo.triggerLevelUpCompleted(user, UPGRADE_ID);

        var captor = ArgumentCaptor.forClass(UnlockedRelation.class);
        verify(unlockedRelationBo, times(unlockedRelationExists ? 0 : 1)).save(captor.capture());
        if (!unlockedRelationExists) {
            var savedUr = captor.getValue();
            assertThat(savedUr.getRelation()).isEqualTo(or);
            assertThat(savedUr.getUser()).isEqualTo(user);
        }
        var newOuCaptor = ArgumentCaptor.forClass(ObtainedUpgrade.class);
        verify(obtainedUpgradeBo, times(!obtainedUpgradeExists || !unlockedRelationExists ? 1 : 0)).save(newOuCaptor.capture());
        if (!obtainedUpgradeExists) {
            var newOu = newOuCaptor.getValue();
            assertThat(newOu.getLevel()).isZero();
            assertThat(newOu.getUpgrade()).isEqualTo(upgrade);
            assertThat(newOu.getUser()).isEqualTo(user);
            assertThat(newOu.isAvailable()).isTrue();
        }
        if (obtainedUpgradeExists && !unlockedRelationExists) {
            assertThat(obtainedUpgrade.isAvailable()).isTrue();
        }


    }

    private void checkUnlockedUpgrade(ObjectRelation or, UserStorage user) {
        verify(obtainedUpgradeRepository, times(1)).existsByUserIdAndUpgradeId(user.getId(), or.getReferenceId());
    }

    private void checkRegisterObtainedUpgrade(ObjectRelation or, Upgrade upgrade, UserStorage user) {
        checkUnlockedUpgrade(or, user);
        verify(upgradeBo, times(1)).findById(upgrade.getId());
        var captor = ArgumentCaptor.forClass(ObtainedUpgrade.class);
        verify(obtainedUpgradeBo, times(1)).save(captor.capture());
        var savedObtainedUpgrade = captor.getValue();
        assertThat(savedObtainedUpgrade.getLevel()).isZero();
        assertThat(savedObtainedUpgrade.getUpgrade()).isEqualTo(upgrade);
        assertThat(savedObtainedUpgrade.getUser()).isEqualTo(user);
        assertThat(savedObtainedUpgrade.isAvailable()).isTrue();
    }

    private static Stream<Arguments> findBoByRequirement_arguments() {
        return Stream.of(
                Arguments.of(UPGRADE_LEVEL, UpgradeBo.class),
                Arguments.of(UPGRADE_LEVEL_LOWER_THAN, UpgradeBo.class),
                Arguments.of(HAVE_UNIT, UnitBo.class),
                Arguments.of(UNIT_AMOUNT, UnitBo.class),
                Arguments.of(BEEN_RACE, FactionBo.class),
                Arguments.of(HAVE_SPECIAL_LOCATION, SpecialLocationBo.class),
                Arguments.of(HAVE_SPECIAL_ENABLED, TimeSpecialBo.class),
                Arguments.of(HAVE_SPECIAL_AVAILABLE, TimeSpecialBo.class),
                Arguments.of(HOME_GALAXY, GalaxyBo.class)
        );
    }
}
