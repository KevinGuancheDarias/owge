package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUniverseIsFull;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.business.PlanetBo.PLANET_OWNED_CHANGE;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenDeployedMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static com.kevinguanchedarias.owgejava.mock.SpecialLocationMock.givenSpecialLocation;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = PlanetBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetRepository.class,
        UserStorageBo.class,
        ObtainedUnitRepository.class,
        SocketIoService.class,
        RequirementBo.class,
        PlanetListBo.class,
        EntityManager.class,
        TransactionUtilService.class,
        ObtainedUnitBo.class,
        MissionRepository.class,
        MissionEventEmitterBo.class,
        ObtainedUnitEventEmitter.class,
        MissionFinderBo.class,
        DtoUtilService.class
})
class PlanetBoTest {
    private final PlanetBo planetBo;

    private final PlanetRepository planetRepository;
    private final UserStorageBo userStorageBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final SocketIoService socketIoService;
    private final RequirementBo requirementBo;
    private final PlanetListBo planetListBo;
    private final EntityManager entityManager;
    private final TransactionUtilService transactionUtilService;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionRepository missionRepository;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionFinderBo missionFinderBo;
    private final DtoUtilService dtoUtilService;

    @Autowired
    PlanetBoTest(
            PlanetBo planetBo,
            PlanetRepository planetRepository,
            UserStorageBo userStorageBo,
            ObtainedUnitRepository obtainedUnitRepository,
            SocketIoService socketIoService,
            RequirementBo requirementBo,
            PlanetListBo planetListBo,
            EntityManager entityManager,
            TransactionUtilService transactionUtilService,
            ObtainedUnitBo obtainedUnitBo,
            MissionRepository missionRepository,
            MissionEventEmitterBo missionEventEmitterBo,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            MissionFinderBo missionFinderBo,
            DtoUtilService dtoUtilService
    ) {
        this.planetBo = planetBo;
        this.planetRepository = planetRepository;
        this.userStorageBo = userStorageBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.socketIoService = socketIoService;
        this.requirementBo = requirementBo;
        this.planetListBo = planetListBo;
        this.entityManager = entityManager;
        this.transactionUtilService = transactionUtilService;
        this.obtainedUnitBo = obtainedUnitBo;
        this.missionRepository = missionRepository;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.missionFinderBo = missionFinderBo;
        this.dtoUtilService = dtoUtilService;
    }

    @Test
    void getEntityManager_should_return_instance() {
        assertThat(planetBo.getEntityManager()).isSameAs(entityManager);
    }

    @Test
    void findRandomPlanet_should_throw_when_galaxy_is_full() {
        assertThatThrownBy(() -> planetBo.findRandomPlanet(GALAXY_ID)).isInstanceOf(SgtBackendUniverseIsFull.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,1,0",
            "5,0,1"
    }, nullValues = "null")
    void findRandomPlanet_should_work(Integer galaxyId, int timesAnyGalaxy, int timesSpecificGalaxy) {
        int count = 4;
        long countLong = 4;
        int planetLocation = 2;
        var planet = givenSourcePlanet();
        given(planetRepository.countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(galaxyId)).willReturn(countLong);
        given(planetRepository.countByOwnerIsNullAndSpecialLocationIsNull()).willReturn(countLong);
        var pageRequestMock = mock(PageRequest.class);
        given(planetRepository.findByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(galaxyId, pageRequestMock)).willReturn(List.of(planet));
        given(planetRepository.findByOwnerIsNullAndSpecialLocationIsNull(pageRequestMock)).willReturn(List.of(planet));
        try (
                var staticMock = mockStatic(RandomUtils.class);
                var pageRequestStaticMock = mockStatic(PageRequest.class)
        ) {
            staticMock.when(() -> RandomUtils.nextInt(0, count)).thenReturn(planetLocation);
            //noinspection ResultOfMethodCallIgnored
            pageRequestStaticMock.when(() -> PageRequest.of(planetLocation, 1)).thenReturn(pageRequestMock);

            var retVal = planetBo.findRandomPlanet(galaxyId);

            assertThat(retVal).isSameAs(planet);

            verify(planetRepository, times(timesSpecificGalaxy)).countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(galaxyId);
            verify(planetRepository, times(timesAnyGalaxy)).countByOwnerIsNullAndSpecialLocationIsNull();
            verify(planetRepository, times(timesSpecificGalaxy)).findByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(galaxyId, pageRequestMock);
            verify(planetRepository, times(timesAnyGalaxy)).findByOwnerIsNullAndSpecialLocationIsNull(pageRequestMock);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void myIsOfUserProperty_should_work(boolean expectedResult) {
        var user = givenUser1();
        given(userStorageBo.findLoggedIn()).willReturn(user);
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(expectedResult);

        assertThat(planetBo.myIsOfUserProperty(SOURCE_PLANET_ID)).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("doLeavePlanet_should_throw_because_cant_leave_arguments")
    void doLeavePlanet_should_throw_because_cant_leave(
            Planet homePlanet,
            boolean isOfUserProperty,
            boolean hasUnitsInPlanet,
            RunningUnitBuildDto runningUnitBuild
    ) {
        given(planetRepository.findOneByIdAndHomeTrue(SOURCE_PLANET_ID)).willReturn(homePlanet);
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(isOfUserProperty);
        given(obtainedUnitRepository.hasUnitsInPlanet(USER_ID_1, SOURCE_PLANET_ID)).willReturn(hasUnitsInPlanet);
        given(missionFinderBo.findRunningUnitBuild(USER_ID_1, (double) SOURCE_PLANET_ID)).willReturn(runningUnitBuild);

        assertThatThrownBy(() -> planetBo.doLeavePlanet(USER_ID_1, SOURCE_PLANET_ID))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessage("ERR_I18N_CAN_NOT_LEAVE_PLANET");
    }

    @ParameterizedTest
    @MethodSource("doLeavePlanet_should_work_arguments")
    void doLeavePlanet_should_work(SpecialLocation specialLocation, int timesTriggerSpecialLocation) {
        var planet = givenSourcePlanet();
        var planetForSocketList = List.of(mock(PlanetDto.class));
        var user = givenUser1();
        planet.setOwner(user);
        planet.setSpecialLocation(specialLocation);
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(true);
        given(planetRepository.findById(SOURCE_PLANET_ID)).willReturn(Optional.of(planet));
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        given(planetRepository.findByOwnerId(USER_ID_1)).willReturn(List.of(planet));
        given(dtoUtilService.convertEntireArray(PlanetDto.class, List.of(planet))).willReturn(planetForSocketList);
        var planetOwnedChangedSocketAnswer = new InvokeSupplierLambdaAnswer<List<PlanetDto>>(2);
        doAnswer(planetOwnedChangedSocketAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(PLANET_OWNED_CHANGE), any());

        planetBo.doLeavePlanet(USER_ID_1, SOURCE_PLANET_ID);

        assertThat(planet.getOwner()).isNull();
        verify(planetRepository, times(1)).save(planet);
        verify(requirementBo, times(timesTriggerSpecialLocation)).triggerSpecialLocation(user, specialLocation);
        verify(planetListBo, times(1)).emitByChangedPlanet(planet);
        assertThat(planetOwnedChangedSocketAnswer.getResult()).isEqualTo(planetForSocketList);
    }

    @SuppressWarnings("ConstantConditions")
    @ParameterizedTest
    @MethodSource("definePlanetAsOwnedBy_should_work_arguments")
    void definePlanetAsOwnedBy_should_work(
            SpecialLocation specialLocation,
            int timesTriggerSpecialLocation,
            ObtainedUnit maybeDeployedUnit,
            int timesMoveUnit,
            int timesRemoveMission
    ) {
        var user = givenUser1();
        var planet = givenSourcePlanet();
        planet.setSpecialLocation(specialLocation);

        var ou = givenObtainedUnit1().toBuilder()
                .targetPlanet(givenTargetPlanet())
                .mission(givenExploreMission())
                .build();
        if (maybeDeployedUnit != null) {
            given(obtainedUnitRepository.findByUserIdAndTargetPlanetAndMissionTypeCode(USER_ID_1, planet, MissionType.DEPLOYED.name()))
                    .willReturn(List.of(maybeDeployedUnit));
        }
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        var planetOwnedChangedSocketAnswer = new InvokeSupplierLambdaAnswer<List<PlanetDto>>(2);
        doAnswer(planetOwnedChangedSocketAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(PLANET_OWNED_CHANGE), any());
        given(planetRepository.findByOwnerId(USER_ID_1)).willReturn(List.of(planet));
        given(dtoUtilService.convertEntireArray(PlanetDto.class, List.of(planet))).willReturn(List.of(mock(PlanetDto.class)));


        planetBo.definePlanetAsOwnedBy(user, List.of(ou), planet);

        verify(planetRepository, times(1)).save(planet);
        verify(obtainedUnitBo, times(timesMoveUnit)).moveUnit(maybeDeployedUnit, USER_ID_1, SOURCE_PLANET_ID);
        verify(missionRepository, times(timesRemoveMission)).delete(maybeDeployedUnit == null ? null : maybeDeployedUnit.getMission());
        verify(requirementBo, times(timesTriggerSpecialLocation)).triggerSpecialLocation(user, specialLocation);
        verify(planetListBo, times(1)).emitByChangedPlanet(planet);
        verify(dtoUtilService, times(1)).convertEntireArray(PlanetDto.class, List.of(planet));
        assertThat(planetOwnedChangedSocketAnswer.getResult()).isNotEmpty();
        verify(missionEventEmitterBo, times(1)).emitEnemyMissionsChange(user);
        obtainedUnitEventEmitter.emitObtainedUnits(user);
    }

    private static Stream<Arguments> doLeavePlanet_should_throw_because_cant_leave_arguments() {
        var homePlanet = givenSourcePlanet();
        var runningUnitBuild = mock(RunningUnitBuildDto.class);
        return Stream.of(
                Arguments.of(homePlanet, true, false, null),
                Arguments.of(null, false, false, null),
                Arguments.of(null, true, true, null),
                Arguments.of(null, true, false, runningUnitBuild)
        );
    }

    private static Stream<Arguments> doLeavePlanet_should_work_arguments() {
        return Stream.of(
                Arguments.of(null, 0),
                Arguments.of(givenSpecialLocation(), 1)
        );
    }

    private static Stream<Arguments> definePlanetAsOwnedBy_should_work_arguments() {
        var deployedMission = givenDeployedMission();
        var deployedUnit = givenObtainedUnit1().toBuilder().mission(deployedMission).build();
        var unitNotDeployed = givenObtainedUnit1();
        return Stream.of(
                Arguments.of(null, 0, null, 0, 0),
                Arguments.of(givenSpecialLocation(), 1, null, 0, 0),
                Arguments.of(null, 0, deployedUnit, 1, 1),
                Arguments.of(null, 0, unitNotDeployed, 1, 0)
        );
    }
}
