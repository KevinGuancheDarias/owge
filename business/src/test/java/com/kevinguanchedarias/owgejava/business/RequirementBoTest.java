package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.requirement.RequirementInternalEventEmitterService;
import com.kevinguanchedarias.owgejava.business.requirement.RequirementSource;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnlockedSpeedImpactGroupService;
import com.kevinguanchedarias.owgejava.business.timespecial.UnlockableTimeSpecialService;
import com.kevinguanchedarias.owgejava.business.unit.UnlockableUnitService;
import com.kevinguanchedarias.owgejava.business.user.UserPlanetLockService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.InvalidConfigurationException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructRequirementBo;
import com.kevinguanchedarias.owgejava.mock.ObjectRelationMock;
import com.kevinguanchedarias.owgejava.repository.*;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.util.ValidationUtilTestHelper;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.ValidationUtil;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum.*;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.FACTION_ID;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenFaction;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_ID;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.givenGalaxy;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.*;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUpgradeMock.givenObtainedUpgrade;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.*;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.*;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.UPGRADE_ID;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.givenUpgrade;
import static com.kevinguanchedarias.owgejava.mock.UpgradeTypeMock.givenUpgradeType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = NonPostConstructRequirementBo.class
)
@MockBean({
        RequirementRepository.class,
        UserStorageRepository.class,
        UnlockedRelationBo.class,
        UpgradeBo.class,
        ObjectRelationBo.class,
        ObjectRelationToObjectRelationBo.class,
        DtoUtilService.class,
        RequirementInformationRepository.class,
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
        GalaxyBo.class,
        UnlockedRelationRepository.class,
        PlanetRepository.class,
        UnlockableUnitService.class,
        UnlockableTimeSpecialService.class,
        UnlockedSpeedImpactGroupService.class,
        RequirementInternalEventEmitterService.class,
        UserPlanetLockService.class
})
@AllArgsConstructor(onConstructor_ = @Autowired)
class RequirementBoTest {
    private final NonPostConstructRequirementBo requirementBo;
    private final RequirementSource requirementSource;
    private final ObjectRelationBo objectRelationBo;
    private final UnlockedRelationBo unlockedRelationBo;
    private final UpgradeBo upgradeBo;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;
    private final RequirementRepository requirementRepository;
    private final UnitRepository unitRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final UserStorageRepository userStorageRepository;
    private final DtoUtilService dtoUtilService;
    private final RequirementInformationRepository requirementInformationRepository;
    private final UnlockedRelationRepository unlockedRelationRepository;
    private final RequirementInternalEventEmitterService requirementInternalEventEmitterService;
    private final UserPlanetLockService userPlanetLockService;

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
    void findBoByRequirement_should_throw_when_unsupported() {
        assertThatThrownBy(() -> requirementBo.findBoByRequirement(UNIMPLEMENTED_PLACEHOLDER))
                .isInstanceOf(SgtBackendNotImplementedException.class);
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
        verify(unlockedRelationRepository, times(1)).save(captor.capture());
        var savedUnlocked = captor.getValue();
        verify(requirementInternalEventEmitterService, times(1)).doNotifyObtainedRelation(savedUnlocked);
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
        verify(unlockedRelationRepository, times(1)).delete(ur);
        verify(obtainedUpgradeRepository, times(isUpgrade ? 1 : 0)).existsByUserIdAndUpgradeId(USER_ID_1, REFERENCE_ID);
        var captor = ArgumentCaptor.forClass(ObtainedUpgrade.class);
        verify(obtainedUpgradeRepository, times(isUpgrade && lostUpgradeRelationExists ? 1 : 0)).save(captor.capture());
        verify(requirementInternalEventEmitterService, times(1)).doNotifyLostRelation(ur);
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
        given(objectRelationBo.findByRequirementTypeAndSecondValue(UPGRADE_LEVEL, UPGRADE_ID))
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
        verify(unlockedRelationRepository, times(unlockedRelationExists ? 0 : 1)).save(captor.capture());
        if (!unlockedRelationExists) {
            var savedUr = captor.getValue();
            assertThat(savedUr.getRelation()).isEqualTo(or);
            assertThat(savedUr.getUser()).isEqualTo(user);
        }
        var newOuCaptor = ArgumentCaptor.forClass(ObtainedUpgrade.class);
        verify(obtainedUpgradeRepository, times(!obtainedUpgradeExists || !unlockedRelationExists ? 1 : 0)).save(newOuCaptor.capture());
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

    @Test
    void findFactionUnitLevelRequirements_should_work() {
        var faction = givenFaction();
        var orUnmatched = givenObjectRelation();
        var orMatched = givenObjectRelation();
        var level = 9;
        orMatched.setRequirements(List.of(
                givenRequirementInformation(UPGRADE_ID, level, UPGRADE_LEVEL),
                givenRequirementInformation(9, HAVE_SPECIAL_ENABLED)
        ));
        orMatched.setObject(givenObjectEntity(ObjectEnum.UNIT));

        var unit = givenUnit1();
        var upgrade = givenUpgrade();
        upgrade.setType(givenUpgradeType());

        given(objectRelationBo.findByRequirementTypeAndSecondValue(BEEN_RACE, FACTION_ID))
                .willReturn(List.of(orUnmatched, orMatched));
        given(objectRelationBo.unboxObjectRelation(orMatched)).willReturn(unit);
        given(upgradeBo.findById(UPGRADE_ID)).willReturn(upgrade);

        var result = requirementBo.findFactionUnitLevelRequirements(faction);

        assertThat(result).hasSize(1);
        var entry = result.get(0);
        assertThat(entry.getRequirements()).hasSize(1);
        assertThat(entry.getUnit().getId()).isEqualTo(UNIT_ID_1);
        var resultRequirements = entry.getRequirements().get(0);
        assertThat(resultRequirements.getUpgrade().getId()).isEqualTo(UPGRADE_ID);
        assertThat(resultRequirements.getLevel()).isEqualTo(level);
    }

    @Test
    void triggerFactionSelection_should_work() {
        var user = givenUser1();
        var or = givenObjectRelation();
        or.setReferenceId(FACTION_ID);
        or.setRequirements(List.of(givenRequirementInformation(FACTION_ID, BEEN_RACE)));
        given(objectRelationBo.findObjectRelationsHavingRequirementType(BEEN_RACE))
                .willReturn(List.of(or));

        requirementBo.triggerFactionSelection(user);

        verify(userStorageRepository, times(1)).isOfFaction(USER_ID_1, FACTION_ID);
    }

    @ParameterizedTest
    @CsvSource({
            GALAXY_ID + ",1",
            "999,0"
    })
    void triggerHomeGalaxySelection_should_work(int userGalaxyId, int expectedUnlockedSaveCalls) {
        var user = givenUser1();
        user.setHomePlanet(givenSourcePlanet());
        user.getHomePlanet().setGalaxy(givenGalaxy());
        user.getHomePlanet().getGalaxy().setId(userGalaxyId);
        var or = givenObjectRelation();
        or.setReferenceId(UPGRADE_ID);
        or.setRequirements(List.of(givenRequirementInformation(GALAXY_ID, HOME_GALAXY)));
        given(objectRelationBo.findObjectRelationsHavingRequirementType(HOME_GALAXY))
                .willReturn(List.of(or));

        requirementBo.triggerHomeGalaxySelection(user);

        verify(unlockedRelationRepository, times(expectedUnlockedSaveCalls)).save(any(UnlockedRelation.class));
    }

    @ParameterizedTest
    @CsvSource({
            "false,40,0",
            "true,40,1",
            "true,20,2"
    })
    void triggerUnitBuildCompletedOrKilled_should_work(boolean isUnitBuilt, long requiredAmount, int unlockedTimes) {
        var user = givenUser1();
        var unit = givenUnit1();
        var orHaveUnit = givenObjectRelation();
        var unitCount = 29L;
        orHaveUnit.setReferenceId(UPGRADE_ID);
        orHaveUnit.setRequirements(List.of(givenRequirementInformation(UNIT_ID_1, HAVE_UNIT)));
        var orHaveAmount = givenObjectRelation();
        orHaveAmount.setReferenceId(UPGRADE_ID + 2);
        orHaveAmount.setRequirements(List.of(givenRequirementInformation(UNIT_ID_1, requiredAmount, UNIT_AMOUNT)));
        given(objectRelationBo.findByRequirementTypeAndSecondValueIn(HAVE_UNIT, List.of((long) UNIT_ID_1)))
                .willReturn(List.of(orHaveUnit));
        given(obtainedUnitRepository.countByUserAndUnit(user, unit)).willReturn(unitCount);
        given(objectRelationBo.findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(UNIT_AMOUNT, UNIT_ID_1, unitCount))
                .willReturn(List.of(orHaveAmount));
        given(unitRepository.getReferenceById(UNIT_ID_1)).willReturn(unit);
        given(obtainedUnitRepository.isBuiltUnit(user, unit)).willReturn(isUnitBuilt);
        doAnswer(new InvokeRunnableLambdaAnswer(1)).when(userPlanetLockService).runLockedForUser(eq(user), any());
        requirementBo.triggerUnitBuildCompletedOrKilled(user, unit);

        verify(obtainedUnitRepository, times(1)).isBuiltUnit(user, unit);
        verify(obtainedUnitRepository, times(2)).countByUserAndUnit(user, unit);
        verify(unlockedRelationRepository, times(unlockedTimes)).save(any(UnlockedRelation.class));
        verify(userPlanetLockService, times(2)).runLockedForUser(eq(user), any());

    }

    @Test
    void addRequirementFromDto_should_work() {
        var input = givenRequirementInformationDto(null);
        var output = givenRequirementInformationDto(1234);
        var or = givenObjectRelation();
        var requirementEntity = givenRequirement(UPGRADE_LEVEL);
        given(objectRelationBo.findObjectRelationOrCreate(DTO_OBJECT_CODE, REFERENCE_ID)).willReturn(or);
        given(requirementInformationRepository.save(any(RequirementInformation.class))).willAnswer(returnsFirstArg());
        given(dtoUtilService.dtoFromEntity(eq(RequirementInformationDto.class), any(RequirementInformation.class))).willReturn(output);
        given(requirementRepository.findOneByCode(REQUIREMENT_CODE)).willReturn(requirementEntity);

        try (var validationUtilMockedStatic = mockStatic(ValidationUtil.class)) {
            var validationUtilTestHelper = ValidationUtilTestHelper.getInstance(validationUtilMockedStatic);
            var result = requirementBo.addRequirementFromDto(input);

            validationUtilTestHelper
                    .assertRequireNotNull(input.getRequirement(), "requirement")
                    .assertRequireNull("requirement.id")
                    .assertRequireNotNull(input.getRelation(), "relation")
                    .assertRequireValidEnumValue(input.getRelation().getObjectCode(), ObjectEnum.class, "relation.objectCode")
                    .assertRequirePositiveNumber(input.getRelation().getReferenceId(), "relation.referenceId")
                    .assertRequireValidEnumValue(input.getRequirement().getCode(), RequirementTypeEnum.class, "requirement.code")
                    .assertRequireNotNull(input.getSecondValue(), "secondValue");
            var savedCaptor = ArgumentCaptor.forClass(RequirementInformation.class);
            verify(requirementInformationRepository, times(1)).save(savedCaptor.capture());
            var saved = savedCaptor.getValue();
            assertThat(saved.getSecondValue()).isEqualTo(REQUIREMENT_INFORMATION_SECOND_VALUE);
            assertThat(saved.getThirdValue()).isEqualTo(REQUIREMENT_INFORMATION_THIRD_VALUE);
            assertThat(saved.getRelation()).isEqualTo(or);
            assertThat(saved.getRequirement()).isEqualTo(requirementEntity);
            assertThat(result).isEqualTo(output);

        }


    }

    private void checkUnlockedUpgrade(ObjectRelation or, UserStorage user) {
        verify(obtainedUpgradeRepository, times(1)).existsByUserIdAndUpgradeId(user.getId(), or.getReferenceId());
    }

    private void checkRegisterObtainedUpgrade(ObjectRelation or, Upgrade upgrade, UserStorage user) {
        checkUnlockedUpgrade(or, user);
        verify(upgradeBo, times(1)).findById(upgrade.getId());
        var captor = ArgumentCaptor.forClass(ObtainedUpgrade.class);
        verify(obtainedUpgradeRepository, times(1)).save(captor.capture());
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
