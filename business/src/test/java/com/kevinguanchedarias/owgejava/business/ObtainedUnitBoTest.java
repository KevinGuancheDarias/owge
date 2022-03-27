package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.InterceptableSpeedGroupMock.givenInterceptableSpeedGroup;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenDeployedMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_COUNT;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_ID;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitTemporalInformationMock.givenObtainedUnitTemporalInformation;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_NAME;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = ObtainedUnitBo.class
)
@MockBean({
        ObtainedUnitRepository.class,
        UserStorageBo.class,
        PlanetBo.class,
        UnitTypeBo.class,
        ImprovementBo.class,
        SocketIoService.class,
        AsyncRunnerBo.class,
        EntityManager.class,
        RequirementBo.class,
        MissionFinderBo.class,
        TaggableCacheManager.class,
        HiddenUnitBo.class,
        SpeedImpactGroupFinderBo.class,
        UserStorageRepository.class,
        ObtainedUnitTemporalInformationRepository.class
})
class ObtainedUnitBoTest extends AbstractBaseBoTest {
    private final ObtainedUnitBo obtainedUnitBo;
    private final EntityManager entityManager;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final PlanetBo planetBo;
    private final MissionFinderBo missionFinderBo;
    private final TaggableCacheManager taggableCacheManager;
    private final HiddenUnitBo hiddenUnitBo;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;
    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;

    @Autowired
    ObtainedUnitBoTest(
            ObtainedUnitBo obtainedUnitBo,
            EntityManager entityManager,
            ObtainedUnitRepository obtainedUnitRepository,
            PlanetBo planetBo,
            MissionFinderBo missionFinderBo,
            TaggableCacheManager taggableCacheManager,
            HiddenUnitBo hiddenUnitBo,
            SpeedImpactGroupFinderBo speedImpactGroupFinderBo,
            ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository
    ) {
        // Some methods has not all branches covered, only touched lines
        this.obtainedUnitBo = obtainedUnitBo;
        this.entityManager = entityManager;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.planetBo = planetBo;
        this.missionFinderBo = missionFinderBo;
        this.taggableCacheManager = taggableCacheManager;
        this.hiddenUnitBo = hiddenUnitBo;
        this.speedImpactGroupFinderBo = speedImpactGroupFinderBo;
        this.obtainedUnitTemporalInformationRepository = obtainedUnitTemporalInformationRepository;
    }

    @Test
    void saveWithChange_should_work() {
        var ou = givenObtainedUnit1();
        var sumValue = 14L;

        assertThat(obtainedUnitBo.saveWithChange(ou, sumValue)).isEqualTo(ou);

        verify(obtainedUnitRepository, times(1)).updateCount(ou, sumValue);
        verify(entityManager, times(1)).refresh(ou);
    }

    @Test
    void moveUnit_should_do_only_save_when_unit_is_part_of_DEPLOYED_mission() {
        var ou = givenObtainedUnit1();
        ou.setTargetPlanet(null);
        ou.setMission(givenDeployedMission());
        given(obtainedUnitRepository.save(ou)).willAnswer(returnsFirstArg());
        given(planetBo.findById(TARGET_PLANET_ID)).willReturn(givenTargetPlanet());

        var result = obtainedUnitBo.moveUnit(ou, USER_ID_1, TARGET_PLANET_ID);

        verify(planetBo, times(1)).isOfUserProperty(anyInt(), anyLong());
        verify(obtainedUnitRepository, times(1)).save(ou);
        assertThat(ou.getTargetPlanet()).isEqualTo(givenTargetPlanet());
        assertThat(result).isSameAs(ou);
    }

    @Test
    void moveUnit_should_assign_a_target_planet() {
        var ou = givenObtainedUnit1();
        var deployedMission = givenDeployedMission();
        ou.setTargetPlanet(null);
        ou.setMission(null);
        given(obtainedUnitRepository.save(ou)).willAnswer(returnsFirstArg());
        given(planetBo.findById(TARGET_PLANET_ID)).willReturn(givenTargetPlanet());
        given(missionFinderBo.findDeployedMissionOrCreate(ou)).willReturn(deployedMission);

        var result = obtainedUnitBo.moveUnit(ou, USER_ID_1, TARGET_PLANET_ID);

        verify(planetBo, times(2)).isOfUserProperty(USER_ID_1, TARGET_PLANET_ID);
        verify(obtainedUnitRepository, times(2)).save(ou);
        assertThat(ou.getTargetPlanet()).isEqualTo(givenTargetPlanet());
        assertThat(result.getMission()).isEqualTo(deployedMission);
        assertThat(result).isSameAs(ou);
    }

    @Test
    void explorePlanetUnits_should_work() {
        var ou = givenObtainedUnit1();
        var invisibleOu = givenObtainedUnit2();
        invisibleOu.setUser(ou.getUser());
        invisibleOu.getUnit().setIsInvisible(true);
        var mission = givenExploreMission();
        var planet = mission.getTargetPlanet();
        given(obtainedUnitRepository.findByExplorePlanet(EXPLORE_MISSION_ID, TARGET_PLANET_ID)).willReturn(List.of(ou, invisibleOu));

        var result = obtainedUnitBo.explorePlanetUnits(mission, planet);
        verify(hiddenUnitBo, times(1)).defineHidden(List.of(ou, invisibleOu), result);
        assertThat(result).hasSize(2);
        var visibleResult = result.get(0);
        var invisibleResult = result.get(1);
        assertThat(visibleResult.getUnit().getName()).isEqualTo(UNIT_NAME);
        assertThat(visibleResult.getCount()).isEqualTo(OBTAINED_UNIT_1_COUNT);
        assertThat(invisibleResult).isNotNull();
        assertThat(invisibleResult.getCount()).isNull();
        assertThat(invisibleResult.getUnit()).isNull();

    }

    @ParameterizedTest
    @CsvSource({
            "true,false",
            "false,true"
    })
    void findCompletedAsDto_should_work(boolean hasSig, boolean hasExpirationId) {
        var ou = givenObtainedUnit1();
        var expirationId = 19L;
        var temporalInformation = givenObtainedUnitTemporalInformation();
        if (hasExpirationId) {
            ou.setExpirationId(expirationId);
        }
        var user = ou.getUser();
        var sig = givenSpeedImpactGroup();
        var isgList = List.of(givenInterceptableSpeedGroup());

        var requirementGroup = RequirementGroup.builder().build();
        var unit = ou.getUnit();
        if (hasSig) {
            unit.setSpeedImpactGroup(sig);
        }
        sig.setRequirementGroups(List.of(requirementGroup));
        unit.setInterceptableSpeedGroups(isgList);
        given(hiddenUnitBo.isHiddenUnit(ou)).willReturn(true);
        given(speedImpactGroupFinderBo.findApplicable(user, unit)).willReturn(sig);
        given(obtainedUnitRepository.findBySourcePlanetNotNullAndMissionNullAndUserId(USER_ID_1))
                .willReturn(List.of(ou));
        given(obtainedUnitTemporalInformationRepository.findById(expirationId)).willReturn(Optional.of(temporalInformation));

        try (var hibernateMock = mockStatic(Hibernate.class)) {
            var result = obtainedUnitBo.findCompletedAsDto(user);

            if (hasSig) {
                hibernateMock.verify(() -> Hibernate.initialize(sig));
            }
            hibernateMock.verify(() -> Hibernate.initialize(isgList));
            verify(entityManager, times(1)).detach(unit);
            verify(hiddenUnitBo, times(1)).isHiddenUnit(ou);
            verify(speedImpactGroupFinderBo, times(hasSig ? 0 : 1)).findApplicable(user, unit);
            assertThat(result)
                    .hasSize(1);
            var ouDto = result.get(0);
            var unitDto = ouDto.getUnit();
            assertThat(unitDto.getIsInvisible()).isTrue();
            assertThat(unitDto.getSpeedImpactGroup()).isNotNull();
            assertThat(unitDto.getSpeedImpactGroup().getRequirementsGroups()).isNull();
            if (hasExpirationId) {
                assertThat(ouDto.getTemporalInformation()).isEqualTo(temporalInformation);
                assertThat(ouDto.getTemporalInformation().getPendingMillis()).isNotNull();
            } else {
                assertThat(ouDto.getTemporalInformation()).isNull();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("saveWithAdding_should_work_parameters")
    void saveWithAdding_should_work(boolean isOfUserProperty, boolean existingOne, Long expirationId, Long ouId) {
        var ou = givenObtainedUnit1();
        var hasExpirationId = expirationId != null;
        ou.setId(ouId);
        ou.setExpirationId(expirationId);
        given(planetBo.isOfUserProperty(USER_ID_1, TARGET_PLANET_ID)).willReturn(isOfUserProperty);
        if (existingOne) {
            given(obtainedUnitRepository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID))
                    .willReturn(ou);
            given(obtainedUnitRepository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(
                    USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, MissionType.DEPLOYED.name())
            ).willReturn(ou);
            given(obtainedUnitRepository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNullAndExpirationId(
                    USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, expirationId)
            ).willReturn(ou);
            given(obtainedUnitRepository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndMissionTypeCodeAndExpirationId(
                    USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, MissionType.DEPLOYED.name(), expirationId)
            ).willReturn(ou);
        }

        obtainedUnitBo.saveWithAdding(USER_ID_1, ou, TARGET_PLANET_ID);

        verify(obtainedUnitRepository, times(isOfUserProperty && !hasExpirationId ? 1 : 0))
                .findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID);

        verify(obtainedUnitRepository, times(!isOfUserProperty && !hasExpirationId ? 1 : 0))
                .findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, MissionType.DEPLOYED.name());
        verify(obtainedUnitRepository, times(isOfUserProperty && hasExpirationId ? 1 : 0)).findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNullAndExpirationId(
                USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, expirationId
        );
        verify(obtainedUnitRepository, times(!isOfUserProperty && hasExpirationId ? 1 : 0)).findOneByUserIdAndUnitIdAndTargetPlanetIdAndMissionTypeCodeAndExpirationId(
                USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, MissionType.DEPLOYED.name(), expirationId
        );
        verify(obtainedUnitRepository, times(!existingOne ? 1 : 0)).save(ou);
        verify(obtainedUnitRepository, times(existingOne ? 1 : 0)).updateCount(ou, OBTAINED_UNIT_1_COUNT);
        verify(entityManager, times(existingOne ? 1 : 0)).refresh(ou);
        verify(obtainedUnitRepository, times(ouId != null ? 1 : 0)).delete(ou);
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ObtainedUnitBo.OBTAINED_UNIT_CACHE_TAG)
                .targetBo(obtainedUnitBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }

    private static Stream<Arguments> saveWithAdding_should_work_parameters() {
        return Stream.of(
                Arguments.of(true, true, null, null),
                Arguments.of(true, true, null, OBTAINED_UNIT_1_ID),
                Arguments.of(false, true, null, null),
                Arguments.of(true, true, 2L, null),
                Arguments.of(false, true, 2L, null),
                Arguments.of(false, false, null, null)
        );
    }
}
