package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitImprovementCalculationService;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.*;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.*;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_NAME;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = ObtainedUnitBo.class
)
@MockBean({
        ObtainedUnitRepository.class,
        UnitTypeBo.class,
        ImprovementBo.class,
        SocketIoService.class,
        AsyncRunnerBo.class,
        EntityManager.class,
        RequirementBo.class,
        MissionFinderBo.class,
        HiddenUnitBo.class,
        ObtainedUnitEventEmitter.class,
        PlanetRepository.class,
        TransactionUtilService.class,
        ObtainedUnitImprovementCalculationService.class,
        UserEventEmitterBo.class,
        TaggableCacheManager.class
})
class ObtainedUnitBoTest {
    private final ObtainedUnitBo obtainedUnitBo;
    private final EntityManager entityManager;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final MissionFinderBo missionFinderBo;
    private final HiddenUnitBo hiddenUnitBo;
    private final PlanetRepository planetRepository;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final TransactionUtilService transactionUtilService;
    private final ImprovementBo improvementBo;
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;
    private final RequirementBo requirementBo;
    private final UserEventEmitterBo userEventEmitterBo;
    private final UnitTypeBo unitTypeBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    ObtainedUnitBoTest(
            ObtainedUnitBo obtainedUnitBo,
            EntityManager entityManager,
            ObtainedUnitRepository obtainedUnitRepository,
            MissionFinderBo missionFinderBo,
            HiddenUnitBo hiddenUnitBo,
            PlanetRepository planetRepository,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            TransactionUtilService transactionUtilService,
            ImprovementBo improvementBo,
            ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService,
            RequirementBo requirementBo,
            UserEventEmitterBo userEventEmitterBo,
            UnitTypeBo unitTypeBo,
            TaggableCacheManager taggableCacheManager
    ) {
        // Some methods have not all branches covered, only touched lines
        this.obtainedUnitBo = obtainedUnitBo;
        this.entityManager = entityManager;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.missionFinderBo = missionFinderBo;
        this.hiddenUnitBo = hiddenUnitBo;
        this.planetRepository = planetRepository;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.transactionUtilService = transactionUtilService;
        this.improvementBo = improvementBo;
        this.obtainedUnitImprovementCalculationService = obtainedUnitImprovementCalculationService;
        this.requirementBo = requirementBo;
        this.userEventEmitterBo = userEventEmitterBo;
        this.unitTypeBo = unitTypeBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Test
    void getRepository_should_work() {
        assertThat(obtainedUnitBo.getRepository()).isEqualTo(obtainedUnitRepository);
    }

    @Test
    void delete_should_work() {
        var entities = List.of(givenObtainedUnit1(), givenObtainedUnit2());

        obtainedUnitBo.delete(entities);

        verify(obtainedUnitEventEmitter, times(1)).emitSideChanges(entities);
        verify(obtainedUnitRepository, times(1)).deleteAll(entities);
    }

    @ParameterizedTest
    @CsvSource({
            "-1,you can go cry",
            "5, obtainedUnit count is less than the amount to"
    })
    void saveWithSubtraction_should_throw(long count, String exceptionMessage) {
        var ou = givenObtainedUnit1();
        ou.setCount(2L);

        assertThatThrownBy(() -> obtainedUnitBo.saveWithSubtraction(ou, count, false))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining(exceptionMessage);
    }

    @ParameterizedTest
    @CsvSource({
            "false,0",
            "true,1"
    })
    void saveWthSubtraction_should_handle_partial_subtraction(boolean handleImprovements, int times) {
        var ou = givenObtainedUnit1();
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        var user = ou.getUser();
        long count = 2;

        obtainedUnitBo.saveWithSubtraction(ou, count, handleImprovements);

        verify(improvementBo, times(times)).clearSourceCache(user, obtainedUnitImprovementCalculationService);
        verify(requirementBo, times(1)).triggerUnitBuildCompletedOrKilled(user, ou.getUnit());
        verify(obtainedUnitRepository, times(1)).updateCount(ou, -count);
        verify(entityManager, times(1)).refresh(ou);
        verify(taggableCacheManager, times(1)).evictByCacheTag(ObtainedUnit.OBTAINED_UNIT_CACHE_TAG_BY_USER, USER_ID_1);
    }

    @Test
    void saveWithSubtraction_should_handle_total_subtraction() {
        var ou = givenObtainedUnit1();

        var retVal = obtainedUnitBo.saveWithSubtraction(ou, OBTAINED_UNIT_1_COUNT, false);

        verify(obtainedUnitRepository, times(1)).delete(ou);
        verify(requirementBo, times(1)).triggerUnitBuildCompletedOrKilled(ou.getUser(), ou.getUnit());
        assertThat(retVal).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "0,0",
            "10,1"
    })
    void saveWithSubtraction_dto_should_work(int unitEnergy, int times) {
        var ou = givenObtainedUnit1();
        ou.getUnit().setEnergy(unitEnergy);
        var user = ou.getUser();
        ou.setUser(user);
        var ouDto = new ObtainedUnitDto();
        ouDto.setId(OBTAINED_UNIT_1_ID);
        ouDto.setCount(4L);
        ouDto.setUserId(USER_ID_1);
        given(obtainedUnitRepository.findById(OBTAINED_UNIT_1_ID)).willReturn(Optional.of(ou));

        obtainedUnitBo.saveWithSubtraction(ouDto, false);

        verify(userEventEmitterBo, times(times)).emitUserData(user);
        verify(unitTypeBo, times(1)).emitUserChange(USER_ID_1);
        verify(obtainedUnitEventEmitter, times(1)).emitObtainedUnits(user);
        verify(obtainedUnitRepository, times(1)).updateCount(eq(ou), anyLong());
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
    void moveUnit_should_handle_save_to_user_owned_planet() {
        var ou = givenObtainedUnit1();
        ou.setSourcePlanet(null);
        ou.setMission(givenExploreMission());
        ou.setTargetPlanet(givenTargetPlanet());
        ou.setFirstDeploymentMission(givenDeployMission());
        ou.setOwnerUnit(givenObtainedUnit2());
        var planet = givenSourcePlanet();
        given(planetRepository.findById(SOURCE_PLANET_ID)).willReturn(Optional.of(planet));
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(true);
        given(obtainedUnitRepository.save(ou)).willReturn(ou);

        var moved = obtainedUnitBo.moveUnit(ou, USER_ID_1, SOURCE_PLANET_ID);

        assertThat(moved).isNotNull();
        var saveCaptor = ArgumentCaptor.forClass(ObtainedUnit.class);
        verify(obtainedUnitRepository, times(1)).save(saveCaptor.capture());
        var saved = saveCaptor.getValue();
        assertThat(saved).isEqualTo(moved);
        assertThat(saved.getSourcePlanet()).isEqualTo(planet);
        assertThat(saved.getTargetPlanet()).isNull();
        assertThat(saved.getMission()).isNull();
        assertThat(saved.getFirstDeploymentMission()).isNull();
        assertThat(saved.getOwnerUnit()).isNull();
    }

    @Test
    void moveUnit_should_do_only_save_when_unit_is_part_of_DEPLOYED_mission() {
        var ou = givenObtainedUnit1();
        ou.setTargetPlanet(null);
        ou.setMission(givenDeployedMission());
        given(obtainedUnitRepository.save(ou)).willAnswer(returnsFirstArg());
        given(planetRepository.findById(TARGET_PLANET_ID)).willReturn(Optional.of(givenTargetPlanet()));

        var result = obtainedUnitBo.moveUnit(ou, USER_ID_1, TARGET_PLANET_ID);

        verify(planetRepository, times(1)).isOfUserProperty(anyInt(), anyLong());
        verify(obtainedUnitRepository, times(1)).save(ou);
        assertThat(ou.getTargetPlanet()).isEqualTo(givenTargetPlanet());
        assertThat(result).isSameAs(ou);
    }

    @ParameterizedTest
    @MethodSource("moveUnit_should_assign_a_target_planet_arguments")
    void moveUnit_should_assign_a_target_planet(Mission originalUnitMission) {
        var ou = givenObtainedUnit1();
        var deployedMission = givenDeployedMission();
        ou.setTargetPlanet(null);
        ou.setMission(originalUnitMission);
        given(obtainedUnitRepository.save(ou)).willAnswer(returnsFirstArg());
        given(planetRepository.findById(TARGET_PLANET_ID)).willReturn(Optional.of(givenTargetPlanet()));
        given(missionFinderBo.findDeployedMissionOrCreate(ou)).willReturn(deployedMission);

        var result = obtainedUnitBo.moveUnit(ou, USER_ID_1, TARGET_PLANET_ID);

        verify(planetRepository, times(2)).isOfUserProperty(USER_ID_1, TARGET_PLANET_ID);
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
    @MethodSource("saveWithAdding_should_work_parameters")
    void saveWithAdding_should_work(boolean isOfUserProperty, boolean existingOne, Long expirationId, Long ouId) {
        var ou = givenObtainedUnit1();
        var hasExpirationId = expirationId != null;
        ou.setId(ouId);
        ou.setExpirationId(expirationId);
        given(planetRepository.isOfUserProperty(USER_ID_1, TARGET_PLANET_ID)).willReturn(isOfUserProperty);
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

    @ParameterizedTest
    @CsvSource(value = {
            "null,true,1,0,0,0",
            "null,false,0,1,0,0",
            "1,true,0,0,1,0",
            "1,false,0,0,0,1"
    }, nullValues = "null")
    void findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission_should_work(
            Long expirationId,
            boolean isDeployedMission,
            int timesDeployed,
            int timesMissionNull,
            int timesDeployedExpirationId,
            int timesMissionNullExpirationId
    ) {
        var deployed = MissionType.DEPLOYED.name();

        assertThatThrownBy(() -> obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(
                USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, expirationId, isDeployedMission
        )).isInstanceOf(NotFoundException.class).hasMessageContaining("dirty hacker");

        verify(obtainedUnitRepository, times(timesDeployed)).findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(
                USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, deployed
        );
        verify(obtainedUnitRepository, times(timesMissionNull)).findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(
                USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID
        );
        verify(obtainedUnitRepository, times(timesDeployedExpirationId)).findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdAndMissionTypeCode(
                USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, expirationId, deployed
        );
        verify(obtainedUnitRepository, times(timesMissionNullExpirationId))
                .findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdAndMissionIsNull(
                        USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, expirationId
                );
    }

    @Test
    void findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission_should_return_val() {
        var ou = givenObtainedUnit1();
        given(obtainedUnitRepository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(
                USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID
        )).willReturn(ou);

        assertThat(obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(USER_ID_1, UNIT_ID_1, TARGET_PLANET_ID, null, false))
                .isEqualTo(ou);

    }

    @Test
    void order_should_work() {
        assertThat(obtainedUnitBo.order())
                .isEqualTo(ObtainedUnitBo.OBTAINED_UNIT_USER_DELETE_ORDER)
                .isLessThan(UnitMissionBo.UNIT_MISSION_USER_DELETE_ORDER);
    }

    @Test
    void doDeleteUser_should_work() {
        var user = givenUser1();

        obtainedUnitBo.doDeleteUser(user);

        verify(obtainedUnitRepository, times(1)).deleteByUser(user);
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

    private static Stream<Arguments> moveUnit_should_assign_a_target_planet_arguments() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(givenExploreMission())
        );
    }
}
