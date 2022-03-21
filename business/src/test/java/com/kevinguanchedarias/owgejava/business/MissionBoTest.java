package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.builder.ExceptionBuilder;
import com.kevinguanchedarias.owgejava.business.mission.MissionConfigurationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUnitBuildAlreadyRunningException;
import com.kevinguanchedarias.owgejava.exception.SgtMissionRegistrationException;
import com.kevinguanchedarias.owgejava.mock.MissionMock;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenEntity;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.BUILD_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenBuildMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenMissionType;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.OBJECT_RELATION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_COUNT;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.SOURCE_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = MissionBo.class
)
@MockBean({
        MissionRepository.class,
        ObtainedUpgradeBo.class,
        ObjectRelationBo.class,
        UpgradeBo.class,
        UserStorageBo.class,
        MissionTypeRepository.class,
        ImprovementBo.class,
        RequirementBo.class,
        UnlockedRelationBo.class,
        UnitBo.class,
        ObtainedUnitBo.class,
        PlanetBo.class,
        ExceptionUtilService.class,
        UnitTypeBo.class,
        MissionConfigurationBo.class,
        SocketIoService.class,
        MissionReportBo.class,
        MissionSchedulerService.class,
        EntityManager.class,
        ConfigurationBo.class,
        AsyncRunnerBo.class,
        TransactionUtilService.class,
        TaggableCacheManager.class,
        HiddenUnitBo.class,
        PlanetLockUtilService.class
})
class MissionBoTest {
    private final MissionBo missionBo;
    private final PlanetBo planetBo;
    private final MissionRepository missionRepository;
    private final SocketIoService socketIoService;
    private final HiddenUnitBo hiddenUnitBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final PlanetLockUtilService planetLockUtilService;
    private final TransactionUtilService transactionUtilService;
    private final RequirementBo requirementBo;
    private final ImprovementBo improvementBo;
    private final ObjectRelationBo objectRelationBo;
    private final UserStorageBo userStorageBo;
    private final ExceptionUtilService exceptionUtilService;
    private final UnitBo unitBo;
    private final ConfigurationBo configurationBo;
    private final MissionTypeRepository missionTypeRepository;
    private final MissionSchedulerService missionSchedulerService;
    private final EntityManager entityManager;
    private final UnitTypeBo unitTypeBo;

    @Autowired
    public MissionBoTest(
            MissionBo missionBo,
            PlanetBo planetBo,
            MissionRepository missionRepository,
            SocketIoService socketIoService,
            HiddenUnitBo hiddenUnitBo,
            ObtainedUnitBo obtainedUnitBo,
            PlanetLockUtilService planetLockUtilService,
            TransactionUtilService transactionUtilService,
            RequirementBo requirementBo,
            ImprovementBo improvementBo,
            ObjectRelationBo objectRelationBo,
            UserStorageBo userStorageBo,
            ExceptionUtilService exceptionUtilService,
            UnitBo unitBo,
            ConfigurationBo configurationBo,
            MissionTypeRepository missionTypeRepository,
            MissionSchedulerService missionSchedulerService,
            EntityManager entityManager,
            UnitTypeBo unitTypeBo
    ) {
        this.missionBo = missionBo;
        this.planetBo = planetBo;
        this.missionRepository = missionRepository;
        this.socketIoService = socketIoService;
        this.hiddenUnitBo = hiddenUnitBo;
        this.obtainedUnitBo = obtainedUnitBo;
        this.planetLockUtilService = planetLockUtilService;
        this.transactionUtilService = transactionUtilService;
        this.requirementBo = requirementBo;
        this.improvementBo = improvementBo;
        this.objectRelationBo = objectRelationBo;
        this.userStorageBo = userStorageBo;
        this.exceptionUtilService = exceptionUtilService;
        this.unitBo = unitBo;
        this.configurationBo = configurationBo;
        this.missionTypeRepository = missionTypeRepository;
        this.missionSchedulerService = missionSchedulerService;
        this.entityManager = entityManager;
        this.unitTypeBo = unitTypeBo;
    }

    @Test
    void findEnemyRunningMissions_should_work() {
        var mission1Id = 192091L;
        var mission2Id = 29892L;
        var userPlanet1 = Planet.builder().id(14L).build();
        var userPlanet2 = Planet.builder().id(19L).build();
        var user = givenUser1();
        var user2 = givenUser2();
        var exploredPlanet = Planet.builder().id(190L).build();
        var involvedUnit = givenObtainedUnit1();
        var invisibleInvolvedUnit = givenObtainedUnit2();
        invisibleInvolvedUnit.getUnit().setIsInvisible(true);
        var involvedUnits = List.of(involvedUnit, invisibleInvolvedUnit);
        var missionWithExploredPlanet = Mission.builder()
                .id(mission1Id)
                .sourcePlanet(exploredPlanet)
                .targetPlanet(givenTargetPlanet())
                .user(user2)
                .involvedUnits(involvedUnits)
                .type(MissionMock.givenMissionType(MissionType.EXPLORE))
                .build();
        var missionWithoutExploredPlanet = Mission.builder()
                .id(mission2Id)
                .sourcePlanet(Planet.builder().id(40L).build())
                .targetPlanet(givenTargetPlanet())
                .user(user2)
                .involvedUnits(involvedUnits)
                .type(MissionMock.givenMissionType(MissionType.EXPLORE))
                .build();
        given(missionRepository.findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(any(), any()))
                .willReturn(List.of(missionWithExploredPlanet, missionWithoutExploredPlanet));
        given(planetBo.findPlanetsByUser(user)).willReturn(List.of(userPlanet1, userPlanet2));
        given(planetBo.isExplored(user, exploredPlanet)).willReturn(true);

        var result = missionBo.findEnemyRunningMissions(user);

        verify(planetBo, times(1)).findPlanetsByUser(user);
        verify(missionRepository, times(1))
                .findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(List.of(userPlanet1, userPlanet2), user);
        verify(planetBo, times(2)).isExplored(eq(user), any());
        assertThat(result).hasSize(2);
        var missionResult1 = result.get(0);
        var missionResult2 = result.get(1);
        assertThat(missionResult1.getSourcePlanet()).isNotNull();
        assertThat(missionResult1.getUser()).isNotNull();
        assertThat(missionResult2.getSourcePlanet()).isNull();
        assertThat(missionResult2.getUser()).isNull();
        Stream.concat(missionResult1.getInvolvedUnits().stream(), missionResult2.getInvolvedUnits().stream()).forEach(involved -> {
            assertThat(involved.getSourcePlanet()).isNull();
            assertThat(involved.getTargetPlanet()).isNull();
        });
        var units = missionResult1.getInvolvedUnits();
        var visibleUnitResult = units.get(0);
        var invisibleUnitResult = units.get(1);
        assertThat(visibleUnitResult.getUnit()).isNotNull();
        assertThat(visibleUnitResult.getCount()).isEqualTo(OBTAINED_UNIT_1_COUNT);
        assertThat(invisibleUnitResult.getUnit()).isNull();
        assertThat(invisibleUnitResult.getCount()).isNull();
        verify(hiddenUnitBo, times(2)).defineHidden(eq(involvedUnits), anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void emitEnemyMissionsChange_should_work() {
        var mission1Id = 192091L;
        var mission2Id = 29892L;
        var userPlanet1 = Planet.builder().id(14L).build();
        var userPlanet2 = Planet.builder().id(19L).build();
        var user = givenUser1();
        var user2 = givenUser2();
        var exploredPlanet = Planet.builder().id(190L).build();
        var involvedUnit = givenObtainedUnit1();
        var invisibleInvolvedUnit = givenObtainedUnit2();
        invisibleInvolvedUnit.getUnit().setIsInvisible(true);
        var involvedUnits = List.of(involvedUnit, invisibleInvolvedUnit);
        var missionWithExploredPlanet = Mission.builder()
                .id(mission1Id)
                .sourcePlanet(exploredPlanet)
                .targetPlanet(givenTargetPlanet())
                .user(user2)
                .involvedUnits(involvedUnits)
                .type(MissionMock.givenMissionType(MissionType.EXPLORE))
                .build();
        var missionWithoutExploredPlanet = Mission.builder()
                .id(mission2Id)
                .sourcePlanet(Planet.builder().id(40L).build())
                .targetPlanet(givenTargetPlanet())
                .user(user2)
                .involvedUnits(involvedUnits)
                .type(MissionMock.givenMissionType(MissionType.EXPLORE))
                .build();
        given(missionRepository.findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(any(), any()))
                .willReturn(List.of(missionWithExploredPlanet, missionWithoutExploredPlanet));
        given(planetBo.findPlanetsByUser(user)).willReturn(List.of(userPlanet1, userPlanet2));
        given(planetBo.isExplored(user, exploredPlanet)).willReturn(true);
        List<List<UnitRunningMissionDto>> messageResultContainer = new ArrayList<>();
        doAnswer(answer -> {
            messageResultContainer.add((List<UnitRunningMissionDto>) answer.getArgument(2, Supplier.class).get());
            return null;
        }).when(socketIoService).sendMessage(any(UserStorage.class), any(), any());

        missionBo.emitEnemyMissionsChange(user);

        var result = messageResultContainer.get(0);
        verify(planetBo, times(1)).findPlanetsByUser(user);
        verify(missionRepository, times(1))
                .findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(List.of(userPlanet1, userPlanet2), user);
        verify(planetBo, times(2)).isExplored(eq(user), any());
        assertThat(result).hasSize(2);
        var missionResult1 = result.get(0);
        var missionResult2 = result.get(1);
        assertThat(missionResult1.getSourcePlanet()).isNotNull();
        assertThat(missionResult1.getUser()).isNotNull();
        assertThat(missionResult2.getSourcePlanet()).isNull();
        assertThat(missionResult2.getUser()).isNull();
        Stream.concat(missionResult1.getInvolvedUnits().stream(), missionResult2.getInvolvedUnits().stream()).forEach(involved -> {
            assertThat(involved.getSourcePlanet()).isNull();
            assertThat(involved.getTargetPlanet()).isNull();
        });
        var units = missionResult1.getInvolvedUnits();
        var visibleUnitResult = units.get(0);
        var invisibleUnitResult = units.get(1);
        assertThat(visibleUnitResult.getUnit()).isNotNull();
        assertThat(visibleUnitResult.getCount()).isEqualTo(OBTAINED_UNIT_1_COUNT);
        assertThat(invisibleUnitResult.getUnit()).isNull();
        assertThat(invisibleUnitResult.getCount()).isNull();
    }

    @Test
    void registerBuildUnit_should_throw_if_mission_already_going() {
        var buildMission = givenBuildMission();
        given(missionRepository.findByUserIdAndTypeCodeAndMissionInformationValue
                (USER_ID_1, MissionType.BUILD_UNIT.name(), (double) SOURCE_PLANET_ID)
        ).willReturn(buildMission);
        given(obtainedUnitBo.findByMissionId(BUILD_MISSION_ID)).willReturn(List.of(givenObtainedUnit1()));
        given(objectRelationBo.unboxObjectRelation(buildMission.getMissionInformation().getRelation()))
                .willReturn(givenUnit1());
        given(planetBo.findById(SOURCE_PLANET_ID)).willReturn(givenSourcePlanet());

        assertThatThrownBy(() -> missionBo.registerBuildUnit(USER_ID_1, SOURCE_PLANET_ID, UNIT_ID_1, OBTAINED_UNIT_1_COUNT))
                .isInstanceOf(SgtBackendUnitBuildAlreadyRunningException.class);
    }

    @Test
    void registerBuildUnit_should_throw_if_mission_limit_reached() {
        var runningCount = 28;
        var groupedImprovementMock = mock(GroupedImprovement.class);
        var exceptionBuilderMock = mock(ExceptionBuilder.class);
        var exception = new SgtBackendInvalidInputException("FOO");
        var relation = givenObjectRelation();
        given(userStorageBo.findById(USER_ID_1)).willReturn(givenUser1());
        given(missionRepository.countByUserIdAndResolvedFalse(USER_ID_1)).willReturn(runningCount);
        given(improvementBo.findUserImprovement(givenUser1())).willReturn(groupedImprovementMock);
        given(groupedImprovementMock.getMoreMisions()).willReturn(20F);
        given(exceptionUtilService.createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_MISSION_LIMIT_EXCEEDED"))
                .willReturn(exceptionBuilderMock);
        given(exceptionBuilderMock.withDeveloperHintDoc(any(), any(), any())).willReturn(exceptionBuilderMock);
        given(exceptionBuilderMock.build()).willReturn(exception);
        given(objectRelationBo.findOneByObjectTypeAndReferenceId(ObjectEnum.UNIT, UNIT_ID_1)).willReturn(relation);

        assertThatThrownBy(() -> missionBo.registerBuildUnit(USER_ID_1, SOURCE_PLANET_ID, UNIT_ID_1, OBTAINED_UNIT_1_COUNT))
                .isEqualTo(exception);
        verify(objectRelationBo, times(1)).checkIsUnlocked(USER_ID_1, OBJECT_RELATION_ID);
    }

    @ParameterizedTest
    @CsvSource({
            "true,1",
            "false," + OBTAINED_UNIT_1_COUNT
    })
    void registerBuildUnit_should_throw_if_no_resources(boolean isUnique, long targetCount) {
        var runningCount = 0;
        var groupedImprovementMock = mock(GroupedImprovement.class);
        var relation = givenObjectRelation();
        var user = givenUser1();
        var unit = givenUnit1();
        unit.setIsUnique(isUnique);
        var resourceRequirementsMock = mock(ResourceRequirementsPojo.class);
        given(userStorageBo.findById(USER_ID_1)).willReturn(user);
        given(missionRepository.countByUserIdAndResolvedFalse(USER_ID_1)).willReturn(runningCount);
        given(improvementBo.findUserImprovement(user)).willReturn(groupedImprovementMock);
        given(groupedImprovementMock.getMoreMisions()).willReturn(20F);
        given(objectRelationBo.findOneByObjectTypeAndReferenceId(ObjectEnum.UNIT, UNIT_ID_1)).willReturn(relation);
        given(unitBo.findByIdOrDie(UNIT_ID_1)).willReturn(unit);
        given(unitBo.calculateRequirements(unit, targetCount)).willReturn(resourceRequirementsMock);

        assertThatThrownBy(() -> missionBo.registerBuildUnit(USER_ID_1, SOURCE_PLANET_ID, UNIT_ID_1, OBTAINED_UNIT_1_COUNT))
                .isInstanceOf(SgtMissionRegistrationException.class);
        verify(objectRelationBo, times(1)).checkIsUnlocked(USER_ID_1, OBJECT_RELATION_ID);
        verify(unitBo, times(1)).checkIsUniqueBuilt(user, unit);
        verify(resourceRequirementsMock, times(1)).canRun(eq(user), any(UserStorageBo.class));
        verify(planetBo, times(1)).myCheckIsOfUserProperty(SOURCE_PLANET_ID);
    }

    @ParameterizedTest
    @CsvSource({
            "TRUE,3",
            "FALSE,20"
    })
    void registerBuildUnit_should_work(String zeroTimeConfigValue, double expectedTime) {
        var runningCount = 0;
        var groupedImprovementMock = mock(GroupedImprovement.class);
        var relation = givenObjectRelation();
        var user = givenUser1();
        user.setPrimaryResource(100D);
        user.setSecondaryResource(200D);
        var unit = givenUnit1();
        var resourceRequirements = new ResourceRequirementsPojo();
        var baseRequiredTime = 18D;
        resourceRequirements.setRequiredTime(baseRequiredTime);
        resourceRequirements.setRequiredPrimary(40D);
        resourceRequirements.setRequiredSecondary(70D);
        var resourceRequirementsSpy = spy(resourceRequirements);
        var moreUnitBuildSpeed = 10F;
        var afterImprovementsTime = 20D;
        var missionType = givenMissionType(MissionType.BUILD_UNIT);
        given(userStorageBo.findById(USER_ID_1)).willReturn(user);
        given(missionRepository.countByUserIdAndResolvedFalse(USER_ID_1)).willReturn(runningCount);
        given(improvementBo.findUserImprovement(user)).willReturn(groupedImprovementMock);
        given(groupedImprovementMock.getMoreMisions()).willReturn(20F);
        given(objectRelationBo.findOneByObjectTypeAndReferenceId(ObjectEnum.UNIT, UNIT_ID_1)).willReturn(relation);
        given(unitBo.findByIdOrDie(UNIT_ID_1)).willReturn(unit);
        given(unitBo.calculateRequirements(any(), any())).willReturn(resourceRequirementsSpy);
        doReturn(true).when(resourceRequirementsSpy).canRun(eq(user), any(UserStorageBo.class));
        given(groupedImprovementMock.getMoreUnitBuildSpeed()).willReturn(moreUnitBuildSpeed);
        given(improvementBo.computeImprovementValue(baseRequiredTime, moreUnitBuildSpeed, false))
                .willReturn(afterImprovementsTime);
        given(configurationBo.findOrSetDefault("ZERO_BUILD_TIME", "TRUE"))
                .willReturn(new Configuration("FOO", zeroTimeConfigValue));
        given(missionTypeRepository.findOneByCode(MissionType.BUILD_UNIT.name()))
                .willReturn(Optional.of(missionType));
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        given(planetBo.findById(SOURCE_PLANET_ID)).willReturn(givenSourcePlanet());

        var result = missionBo.registerBuildUnit(USER_ID_1, SOURCE_PLANET_ID, UNIT_ID_1, OBTAINED_UNIT_1_COUNT);

        assertThat(result.getRequiredTime()).isEqualTo(expectedTime);
        assertThat(result.getRequiredPrimary()).isEqualTo(resourceRequirements.getRequiredPrimary());
        assertThat(result.getRequiredSecondary()).isEqualTo(resourceRequirements.getRequiredSecondary());
        assertThat(result.getType()).isEqualTo(MissionType.BUILD_UNIT);
        verify(userStorageBo, times(1)).save(user);
        verify(missionRepository, times(1)).save(any());
        var captor = ArgumentCaptor.forClass(ObtainedUnit.class);
        verify(obtainedUnitBo, times(1)).save(captor.capture());
        var savedOu = captor.getValue();
        verify(missionSchedulerService, times(1)).scheduleMission(eq(missionBo.getGroupName()), any());
        verify(entityManager, times(1)).refresh(savedOu);
        verify(entityManager, times(1)).refresh(any(Mission.class));
        verify(socketIoService, times(1)).sendMessage(eq(USER_ID_1), eq(MissionBo.MISSIONS_COUNT_CHANGE), any());
        verify(socketIoService, times(1)).sendMessage(eq(USER_ID_1), eq(MissionBo.UNIT_BUILD_MISSION_CHANGE), any());
        verify(unitTypeBo, times(1)).emitUserChange(USER_ID_1);
    }

    @ParameterizedTest
    @MethodSource("processBuildUnit_parameters")
    void processBuildUnit_should_work(Improvement improvement, int expectClearImprovementCacheInvocations) {
        var buildMission = givenBuildMission();
        var sourcePlanet = givenSourcePlanet();
        var ou = givenObtainedUnit1();
        var runningMissionsCount = 92;
        ou.setSourcePlanet(null);
        ou.getUnit().setImprovement(improvement);
        given(missionRepository.findById(BUILD_MISSION_ID)).willReturn(Optional.of(buildMission));
        given(planetBo.findById(SOURCE_PLANET_ID)).willReturn(sourcePlanet);
        given(obtainedUnitBo.findByMissionId(BUILD_MISSION_ID)).willReturn(List.of(ou));
        doAnswer(new InvokeRunnableLambdaAnswer(1)).when(planetLockUtilService)
                .doInsideLockById(eq(List.of(SOURCE_PLANET_ID)), any());
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        var supplierAnswerForBuildChange = new InvokeSupplierLambdaAnswer<List<RunningUnitBuildDto>>(2);
        doAnswer(supplierAnswerForBuildChange).when(socketIoService)
                .sendMessage(eq(USER_ID_1), eq(MissionBo.UNIT_BUILD_MISSION_CHANGE), any());
        var supplierAnswerForMissionCountChange = new InvokeSupplierLambdaAnswer<Integer>(2);
        doAnswer(supplierAnswerForMissionCountChange).when(socketIoService)
                .sendMessage(eq(USER_ID_1), eq(MissionBo.MISSIONS_COUNT_CHANGE), any());
        runFindBuildMissionsGiven();
        given(missionRepository.countByUserIdAndResolvedFalse(USER_ID_1)).willReturn(runningMissionsCount);

        missionBo.processBuildUnit(BUILD_MISSION_ID);
        var missionCountChange = supplierAnswerForMissionCountChange.getResult();
        var missionChange = supplierAnswerForBuildChange.getResult();

        verify(missionRepository, times(2)).findById(BUILD_MISSION_ID);
        verify(planetBo, times(2)).findById(SOURCE_PLANET_ID);
        verify(obtainedUnitBo, times(2)).findByMissionId(BUILD_MISSION_ID);
        assertThat(ou.getSourcePlanet()).isEqualTo(sourcePlanet);
        verify(obtainedUnitBo, times(1)).moveUnit(ou, USER_ID_1, SOURCE_PLANET_ID);
        verify(requirementBo, times(1)).triggerUnitBuildCompletedOrKilled(ou.getUser(), ou.getUnit());
        verify(missionRepository, times(1)).delete(buildMission);
        verify(improvementBo, times(expectClearImprovementCacheInvocations)).clearSourceCache(eq(ou.getUser()), any(ObtainedUnitBo.class));
        verifyFindBuildMissions(missionChange);
        assertThat(missionCountChange).isEqualTo(runningMissionsCount);
    }

    @Test
    void processBuildUnit_should_do_nothing_if_mission_id_null(CapturedOutput capturedOutput) {
        missionBo.processBuildUnit(BUILD_MISSION_ID);

        verify(missionRepository, times(1)).findById(BUILD_MISSION_ID);
        verify(planetLockUtilService, never()).doInsideLock(any(), any());
        verify(planetBo, never()).findById(any());
        assertThat(capturedOutput.getOut()).contains(MissionBo.MISSION_NOT_FOUND);
    }

    private void runFindBuildMissionsGiven() {
        given(missionRepository.findByUserIdAndTypeCodeAndResolvedFalse(USER_ID_1, MissionType.BUILD_UNIT.name()))
                .willReturn(List.of(givenBuildMission()));
        given(objectRelationBo.unboxObjectRelation(givenObjectRelation())).willReturn(givenUnit1());
    }

    private void verifyFindBuildMissions(List<RunningUnitBuildDto> result) {
        assertThat(result).hasSize(1);
        verifyFindBuildMissions(result.get(0));
    }

    private void verifyFindBuildMissions(RunningUnitBuildDto runningUnitBuildDto) {
        assertThat(runningUnitBuildDto.getUnit().getId()).isEqualTo(UNIT_ID_1);
        assertThat(runningUnitBuildDto.getMissionId()).isEqualTo(BUILD_MISSION_ID);
        assertThat(runningUnitBuildDto.getSourcePlanet().getId()).isEqualTo(SOURCE_PLANET_ID);
        assertThat(runningUnitBuildDto.getCount()).isEqualTo(OBTAINED_UNIT_1_COUNT);
    }

    private static Stream<Arguments> processBuildUnit_parameters() {
        return Stream.of(
                Arguments.of(null, 0),
                Arguments.of(givenEntity(), 1)
        );
    }
}
