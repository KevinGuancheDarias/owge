package com.kevinguanchedarias.owgejava.business;


import com.kevinguanchedarias.owgejava.business.mission.MissionConfigurationBo;
import com.kevinguanchedarias.owgejava.business.mission.attack.AttackMissionManagerBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.AllianceMock.givenAlliance;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenAttackObtainedUnit;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenAttackUserInformation;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenFullAttackInformation;
import static com.kevinguanchedarias.owgejava.mock.InterceptableSpeedGroupMock.givenInterceptableSpeedGroup;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.ATTACK_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.CONQUEST_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.GATHER_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenAttackMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenConquestMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenGatherMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenMissionType;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenRawMission;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.givenReport;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_2;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitMissionBo.class
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
        ConfigurationBo.class,
        ImageStoreBo.class,
        PlanetListBo.class,
        AsyncRunnerBo.class,
        EntityManager.class,
        MissionBo.class,
        AuditBo.class,
        CriticalAttackBo.class,
        AttackMissionManagerBo.class,
        ObtainedUnitRepository.class,
        AllianceBo.class,
        TransactionUtilService.class,
        SpeedImpactGroupBo.class
})
class UnitMissionBoTest {
    private static final int ALLY_ID = 19282;

    private final UnitMissionBo unitMissionBo;
    private final PlanetBo planetBo;
    private final AttackMissionManagerBo attackMissionManagerBo;
    private final MissionRepository missionRepository;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionReportBo missionReportBo;
    private final MissionTypeRepository missionTypeRepository;
    private final RequirementBo requirementBo;
    private final PlanetListBo planetListBo;
    private final TransactionUtilService transactionUtilService;
    private final MissionBo missionBo;
    private final SpeedImpactGroupBo speedImpactGroupBo;
    private final AllianceBo allianceBo;
    private final ConfigurationBo configurationBo;

    @Autowired
    public UnitMissionBoTest(
            UnitMissionBo unitMissionBo,
            PlanetBo planetBo,
            AttackMissionManagerBo attackMissionManagerBo,
            MissionRepository missionRepository,
            ObtainedUnitBo obtainedUnitBo,
            MissionReportBo missionReportBo,
            MissionTypeRepository missionTypeRepository,
            RequirementBo requirementBo,
            PlanetListBo planetListBo,
            TransactionUtilService transactionUtilService,
            MissionBo missionBo,
            SpeedImpactGroupBo speedImpactGroupBo,
            AllianceBo allianceBo,
            ConfigurationBo configurationBo
    ) {
        // Notice: Test in this class are not full covering the methods, as they are only testing changed lines
        this.unitMissionBo = unitMissionBo;
        this.planetBo = planetBo;
        this.attackMissionManagerBo = attackMissionManagerBo;
        this.missionRepository = missionRepository;
        this.obtainedUnitBo = obtainedUnitBo;
        this.missionReportBo = missionReportBo;
        this.missionTypeRepository = missionTypeRepository;
        this.requirementBo = requirementBo;
        this.planetListBo = planetListBo;
        this.transactionUtilService = transactionUtilService;
        this.missionBo = missionBo;
        this.speedImpactGroupBo = speedImpactGroupBo;
        this.allianceBo = allianceBo;
        this.configurationBo = configurationBo;
    }

    @Test
    void runUnitMission_check_speed_impact_interception_works() {
        var targetPlanet = givenTargetPlanet();
        var oldPlanetOwner = givenUser2();
        oldPlanetOwner.setAlliance(givenAlliance());
        targetPlanet.setOwner(oldPlanetOwner);
        var conquerorUser = givenUser1();
        var mission = givenConquestMission(givenSourcePlanet(), targetPlanet);
        mission.setUser(conquerorUser);
        mission.setRequiredTime(108D);
        var involvedInMission = List.of(givenObtainedUnit1());
        var attackInformation = givenFullAttackInformation();
        attackInformation.getUsers().get(USER_ID_2).getUnits().get(0).getObtainedUnit().getUnit()
                .setInterceptableSpeedGroups(List.of(givenInterceptableSpeedGroup()));
        attackInformation.getUsers().get(USER_ID_1).getUnits().get(0).getObtainedUnit().getUnit()
                .setInterceptableSpeedGroups(List.of(givenInterceptableSpeedGroup()));
        var missionReport = givenReport();
        given(missionRepository.findById(CONQUEST_MISSION_ID)).willReturn(Optional.of(mission));
        given(obtainedUnitBo.findByMissionId(CONQUEST_MISSION_ID)).willReturn(involvedInMission);
        given(obtainedUnitBo.findInvolvedInAttack(any())).willReturn(
                attackInformation.getUnits().stream().map(AttackObtainedUnit::getObtainedUnit).toList()
        );
        given(speedImpactGroupBo.canIntercept(any(), any())).willReturn(true);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission))
                .willReturn(attackInformation);
        given(missionReportBo.create(any(), anyBoolean(), any())).willReturn(missionReport);
        given(missionTypeRepository.findOneByCode(MissionType.RETURN_MISSION.name())).willReturn(Optional.of(givenMissionType(MissionType.RETURN_MISSION)));
        given(missionReportBo.save(any(MissionReport.class))).willAnswer(returnsFirstArg());

        unitMissionBo.runUnitMission(CONQUEST_MISSION_ID, MissionType.CONQUEST);

        verify(missionRepository, times(1)).findById(CONQUEST_MISSION_ID);
        verify(obtainedUnitBo, times(2)).findByMissionId(CONQUEST_MISSION_ID);
        verify(obtainedUnitBo, times(1)).findInvolvedInAttack(targetPlanet);
        verify(attackMissionManagerBo, times(1)).buildAttackInformation(targetPlanet, mission);
        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(missionReportBo, times(1)).create(any(), eq(true), eq(oldPlanetOwner));
        var missionReportCaptor = ArgumentCaptor.forClass(MissionReport.class);
        verify(missionReportBo, times(1)).save(missionReportCaptor.capture());
        verify(planetBo, never()).save(any(Planet.class));
        verify(obtainedUnitBo, never()).findByUserIdAndTargetPlanetAndMissionTypeCode(any(), any(), eq(MissionType.DEPLOYED));
        verify(requirementBo, never()).triggerSpecialLocation(any(), any());
        verify(planetBo, never()).emitPlanetOwnedChange(any(UserStorage.class));
        verify(planetListBo, never()).emitByChangedPlanet(any());
        verify(allianceBo, times(1)).areEnemies(oldPlanetOwner, conquerorUser);
        var savedReport = missionReportCaptor.getValue();
        assertThat(savedReport.getJsonBody()).contains("I18N_OWNER_NOT_DEFEATED");
    }

    @Test
    void runUnitMission_conquest_should_handle_old_owner_not_defeated() {
        var targetPlanet = givenTargetPlanet();
        var oldPlanetOwner = givenUser2();
        oldPlanetOwner.setAlliance(givenAlliance());
        targetPlanet.setOwner(oldPlanetOwner);
        var conquerorUser = givenUser1();
        var mission = givenConquestMission(givenSourcePlanet(), targetPlanet);
        mission.setUser(conquerorUser);
        mission.setRequiredTime(108D);
        var involvedInMission = List.of(givenObtainedUnit1());
        var attackInformation = givenFullAttackInformation();
        var missionReport = givenReport();
        given(missionRepository.findById(CONQUEST_MISSION_ID)).willReturn(Optional.of(mission));
        given(obtainedUnitBo.findByMissionId(CONQUEST_MISSION_ID)).willReturn(involvedInMission);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission))
                .willReturn(attackInformation);
        given(missionReportBo.create(any(), anyBoolean(), any())).willReturn(missionReport);
        given(missionTypeRepository.findOneByCode(MissionType.RETURN_MISSION.name())).willReturn(Optional.of(givenMissionType(MissionType.RETURN_MISSION)));
        given(missionReportBo.save(any(MissionReport.class))).willAnswer(returnsFirstArg());

        unitMissionBo.runUnitMission(CONQUEST_MISSION_ID, MissionType.CONQUEST);

        verify(missionRepository, times(1)).findById(CONQUEST_MISSION_ID);
        verify(obtainedUnitBo, times(2)).findByMissionId(CONQUEST_MISSION_ID);
        verify(attackMissionManagerBo, times(1)).buildAttackInformation(targetPlanet, mission);
        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(missionReportBo, times(1)).create(any(), eq(true), eq(oldPlanetOwner));
        var missionReportCaptor = ArgumentCaptor.forClass(MissionReport.class);
        verify(missionReportBo, times(1)).save(missionReportCaptor.capture());
        verify(planetBo, never()).save(any(Planet.class));
        verify(obtainedUnitBo, never()).findByUserIdAndTargetPlanetAndMissionTypeCode(any(), any(), eq(MissionType.DEPLOYED));
        verify(requirementBo, never()).triggerSpecialLocation(any(), any());
        verify(planetBo, never()).emitPlanetOwnedChange(any(UserStorage.class));
        verify(planetListBo, never()).emitByChangedPlanet(any());
        var savedReport = missionReportCaptor.getValue();
        assertThat(savedReport.getJsonBody()).contains("I18N_OWNER_NOT_DEFEATED");
    }

    @Test
    void runUnitMission_conquest_should_handle_old_owner_alliance_not_defeated() {
        var targetPlanet = givenTargetPlanet();
        var oldPlanetOwner = givenUser2();
        var oldPlanetOwnerAlliance = givenAlliance();
        var allyUser = new UserStorage();
        allyUser.setId(ALLY_ID);
        allyUser.setAlliance(oldPlanetOwnerAlliance);
        var allyOu = givenAttackObtainedUnit();
        var allyAttackUserInfo = givenAttackUserInformation(allyUser, allyOu);
        oldPlanetOwner.setAlliance(oldPlanetOwnerAlliance);
        targetPlanet.setOwner(oldPlanetOwner);
        var conquerorUser = givenUser1();
        var mission = givenConquestMission(givenSourcePlanet(), targetPlanet);
        mission.setUser(conquerorUser);
        mission.setRequiredTime(108D);
        var involvedInMission = List.of(givenObtainedUnit1());
        var attackInformation = givenFullAttackInformation();
        attackInformation.getUnits().add(allyOu);
        attackInformation.getUsers().put(ALLY_ID, allyAttackUserInfo);
        attackInformation.getUsers().get(USER_ID_2).getUnits().get(0).setFinalCount(0L);
        verify(planetBo, never()).isHomePlanet(any(Planet.class));
        var missionReport = givenReport();
        given(missionRepository.findById(CONQUEST_MISSION_ID)).willReturn(Optional.of(mission));
        given(obtainedUnitBo.findByMissionId(CONQUEST_MISSION_ID)).willReturn(involvedInMission);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission))
                .willReturn(attackInformation);
        given(missionReportBo.create(any(), anyBoolean(), any())).willReturn(missionReport);
        given(missionTypeRepository.findOneByCode(MissionType.RETURN_MISSION.name())).willReturn(Optional.of(givenMissionType(MissionType.RETURN_MISSION)));
        given(missionReportBo.save(any(MissionReport.class))).willAnswer(returnsFirstArg());

        unitMissionBo.runUnitMission(CONQUEST_MISSION_ID, MissionType.CONQUEST);

        verify(missionRepository, times(1)).findById(CONQUEST_MISSION_ID);
        verify(obtainedUnitBo, times(2)).findByMissionId(CONQUEST_MISSION_ID);
        verify(attackMissionManagerBo, times(1)).buildAttackInformation(targetPlanet, mission);
        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(missionReportBo, times(1)).create(any(), eq(true), eq(oldPlanetOwner));
        verify(planetBo, never()).isHomePlanet(any(Planet.class));
        var missionReportCaptor = ArgumentCaptor.forClass(MissionReport.class);
        verify(missionReportBo, times(1)).save(missionReportCaptor.capture());
        verify(planetBo, never()).save(any(Planet.class));
        verify(obtainedUnitBo, never()).findByUserIdAndTargetPlanetAndMissionTypeCode(any(), any(), eq(MissionType.DEPLOYED));
        verify(requirementBo, never()).triggerSpecialLocation(any(), any());
        verify(planetBo, never()).emitPlanetOwnedChange(any(UserStorage.class));
        verify(planetListBo, never()).emitByChangedPlanet(any());
        var savedReport = missionReportCaptor.getValue();
        assertThat(savedReport.getJsonBody()).contains("I18N_ALLIANCE_NOT_DEFEATED");
    }

    @Test
    void runUnitMission_conquest_should_handle_defeated_but_max_planets_reached() {
        var targetPlanet = givenTargetPlanet();
        var oldPlanetOwner = givenUser2();
        var oldPlanetOwnerAlliance = givenAlliance();
        oldPlanetOwner.setAlliance(oldPlanetOwnerAlliance);
        targetPlanet.setOwner(oldPlanetOwner);
        var conquerorUser = givenUser1();
        var mission = givenConquestMission(givenSourcePlanet(), targetPlanet);
        mission.setUser(conquerorUser);
        mission.setRequiredTime(108D);
        var involvedInMission = List.of(givenObtainedUnit1());
        var attackInformation = givenFullAttackInformation();
        attackInformation.getUsers().get(USER_ID_2).getUnits().get(0).setFinalCount(0L);
        verify(planetBo, never()).isHomePlanet(any(Planet.class));
        var missionReport = givenReport();
        given(missionRepository.findById(CONQUEST_MISSION_ID)).willReturn(Optional.of(mission));
        given(obtainedUnitBo.findByMissionId(CONQUEST_MISSION_ID)).willReturn(involvedInMission);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission))
                .willReturn(attackInformation);
        given(missionReportBo.create(any(), anyBoolean(), any())).willReturn(missionReport);
        given(missionTypeRepository.findOneByCode(MissionType.RETURN_MISSION.name())).willReturn(Optional.of(givenMissionType(MissionType.RETURN_MISSION)));
        given(missionReportBo.save(any(MissionReport.class))).willAnswer(returnsFirstArg());
        given(planetBo.hasMaxPlanets(conquerorUser)).willReturn(true);

        unitMissionBo.runUnitMission(CONQUEST_MISSION_ID, MissionType.CONQUEST);

        verify(missionRepository, times(1)).findById(CONQUEST_MISSION_ID);
        verify(obtainedUnitBo, times(2)).findByMissionId(CONQUEST_MISSION_ID);
        verify(attackMissionManagerBo, times(1)).buildAttackInformation(targetPlanet, mission);
        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(missionReportBo, times(1)).create(any(), eq(true), eq(oldPlanetOwner));
        verify(planetBo, never()).isHomePlanet(any(Planet.class));
        var missionReportCaptor = ArgumentCaptor.forClass(MissionReport.class);
        verify(missionReportBo, times(1)).save(missionReportCaptor.capture());
        verify(planetBo, never()).save(any(Planet.class));
        verify(obtainedUnitBo, never()).findByUserIdAndTargetPlanetAndMissionTypeCode(any(), any(), eq(MissionType.DEPLOYED));
        verify(requirementBo, never()).triggerSpecialLocation(any(), any());
        verify(planetBo, never()).emitPlanetOwnedChange(any(UserStorage.class));
        verify(planetListBo, never()).emitByChangedPlanet(any());
        var savedReport = missionReportCaptor.getValue();
        assertThat(savedReport.getJsonBody()).contains("I18N_MAX_PLANETS_EXCEEDED");
    }

    @Test
    void runUnitMission_conquest_should_handle_defeated_but_target_is_home_planet() {
        var targetPlanet = givenTargetPlanet();
        var oldPlanetOwner = givenUser2();
        var oldPlanetOwnerAlliance = givenAlliance();
        oldPlanetOwner.setAlliance(oldPlanetOwnerAlliance);
        targetPlanet.setOwner(oldPlanetOwner);
        var conquerorUser = givenUser1();
        var mission = givenConquestMission(givenSourcePlanet(), targetPlanet);
        mission.setUser(conquerorUser);
        mission.setRequiredTime(108D);
        var involvedInMission = List.of(givenObtainedUnit1());
        var attackInformation = givenFullAttackInformation();
        attackInformation.getUsers().get(USER_ID_2).getUnits().get(0).setFinalCount(0L);
        verify(planetBo, never()).isHomePlanet(any(Planet.class));
        var missionReport = givenReport();
        given(missionRepository.findById(CONQUEST_MISSION_ID)).willReturn(Optional.of(mission));
        given(obtainedUnitBo.findByMissionId(CONQUEST_MISSION_ID)).willReturn(involvedInMission);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission))
                .willReturn(attackInformation);
        given(missionReportBo.create(any(), anyBoolean(), any())).willReturn(missionReport);
        given(missionTypeRepository.findOneByCode(MissionType.RETURN_MISSION.name())).willReturn(Optional.of(givenMissionType(MissionType.RETURN_MISSION)));
        given(missionReportBo.save(any(MissionReport.class))).willAnswer(returnsFirstArg());
        given(planetBo.isHomePlanet(targetPlanet)).willReturn(true);

        unitMissionBo.runUnitMission(CONQUEST_MISSION_ID, MissionType.CONQUEST);

        verify(missionRepository, times(1)).findById(CONQUEST_MISSION_ID);
        verify(obtainedUnitBo, times(2)).findByMissionId(CONQUEST_MISSION_ID);
        verify(attackMissionManagerBo, times(1)).buildAttackInformation(targetPlanet, mission);
        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(missionReportBo, times(1)).create(any(), eq(true), eq(oldPlanetOwner));
        verify(planetBo, times(1)).hasMaxPlanets(conquerorUser);
        verify(planetBo, times(1)).isHomePlanet(any(Planet.class));
        var missionReportCaptor = ArgumentCaptor.forClass(MissionReport.class);
        verify(missionReportBo, times(1)).save(missionReportCaptor.capture());
        verify(planetBo, never()).save(any(Planet.class));
        verify(obtainedUnitBo, never()).findByUserIdAndTargetPlanetAndMissionTypeCode(any(), any(), eq(MissionType.DEPLOYED));
        verify(requirementBo, never()).triggerSpecialLocation(any(), any());
        verify(planetBo, never()).emitPlanetOwnedChange(any(UserStorage.class));
        verify(planetListBo, never()).emitByChangedPlanet(any());
        var savedReport = missionReportCaptor.getValue();
        assertThat(savedReport.getJsonBody()).contains("I18N_CANT_CONQUER_HOME_PLANET");
    }

    @Test
    void runUnitMission_conquest_should_handle_defeated_and_conquer_planet() {
        var targetPlanet = givenTargetPlanet();
        var oldPlanetOwner = givenUser2();
        var oldPlanetOwnerAlliance = givenAlliance();
        oldPlanetOwner.setAlliance(oldPlanetOwnerAlliance);
        targetPlanet.setOwner(oldPlanetOwner);
        var conquerorUser = givenUser1();
        var mission = givenConquestMission(givenSourcePlanet(), targetPlanet);
        mission.setUser(conquerorUser);
        mission.setRequiredTime(108D);
        var involvedInMission = List.of(givenObtainedUnit1());
        var attackInformation = givenFullAttackInformation();
        attackInformation.getUsers().get(USER_ID_2).getUnits().get(0).setFinalCount(0L);
        verify(planetBo, never()).isHomePlanet(any(Planet.class));
        var missionReport = givenReport();
        given(missionRepository.findById(CONQUEST_MISSION_ID)).willReturn(Optional.of(mission));
        given(obtainedUnitBo.findByMissionId(CONQUEST_MISSION_ID)).willReturn(involvedInMission);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission))
                .willReturn(attackInformation);
        given(missionReportBo.create(any(), anyBoolean(), any())).willReturn(missionReport);
        given(missionTypeRepository.findOneByCode(MissionType.RETURN_MISSION.name())).willReturn(Optional.of(givenMissionType(MissionType.RETURN_MISSION)));
        given(missionReportBo.save(any(MissionReport.class))).willAnswer(returnsFirstArg());
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());

        unitMissionBo.runUnitMission(CONQUEST_MISSION_ID, MissionType.CONQUEST);

        verify(missionRepository, times(1)).findById(CONQUEST_MISSION_ID);
        verify(obtainedUnitBo, times(1)).findByMissionId(CONQUEST_MISSION_ID);
        verify(attackMissionManagerBo, times(1)).buildAttackInformation(targetPlanet, mission);
        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(missionReportBo, times(2)).create(any(), eq(true), eq(oldPlanetOwner));
        verify(planetBo, times(1)).hasMaxPlanets(conquerorUser);
        verify(planetBo, times(1)).isHomePlanet(any(Planet.class));
        var missionReportCaptor = ArgumentCaptor.forClass(MissionReport.class);
        verify(missionReportBo, times(1)).save(missionReportCaptor.capture());
        verify(planetBo, times(1)).save(any(Planet.class));
        verify(obtainedUnitBo, times(1)).findByUserIdAndTargetPlanetAndMissionTypeCode(any(), any(), eq(MissionType.DEPLOYED));
        verify(requirementBo, never()).triggerSpecialLocation(any(), any());
        verify(planetBo, times(1)).emitPlanetOwnedChange(oldPlanetOwner);
        verify(planetBo, times(1)).emitPlanetOwnedChange(conquerorUser);
        verify(missionBo, times(1)).emitEnemyMissionsChange(oldPlanetOwner);
        verify(missionBo, times(1)).emitEnemyMissionsChange(conquerorUser);
        verify(planetListBo, times(1)).emitByChangedPlanet(any());
    }

    @Test
    void runUnitMission_gather_triggerAttackIfRequired_should_work_and_not_continue_if_units_death() {
        var targetPlanet = givenTargetPlanet();
        var oldPlanetOwner = givenUser2();
        var oldPlanetOwnerAlliance = givenAlliance();
        oldPlanetOwner.setAlliance(oldPlanetOwnerAlliance);
        targetPlanet.setOwner(oldPlanetOwner);
        var conquerorUser = givenUser1();
        var mission = givenGatherMission();
        mission.setTargetPlanet(targetPlanet);
        mission.setUser(conquerorUser);
        mission.setRequiredTime(108D);
        var involvedInMission = List.of(givenObtainedUnit1());
        var attackInformation = givenFullAttackInformation();
        attackInformation.setRemoved(true);
        attackInformation.getUsersWithDeletedMissions().add(USER_ID_1);
        attackInformation.getUsers().get(USER_ID_2).getUnits().get(0).setFinalCount(0L);
        verify(planetBo, never()).isHomePlanet(any(Planet.class));
        var missionReport = givenReport();
        given(missionRepository.findById(GATHER_MISSION_ID)).willReturn(Optional.of(mission));
        given(obtainedUnitBo.findByMissionId(GATHER_MISSION_ID)).willReturn(involvedInMission);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission))
                .willReturn(attackInformation);
        given(missionReportBo.create(any(), anyBoolean(), any())).willReturn(missionReport);
        given(missionTypeRepository.findOneByCode(MissionType.RETURN_MISSION.name())).willReturn(Optional.of(givenMissionType(MissionType.RETURN_MISSION)));
        given(missionReportBo.save(any(MissionReport.class))).willAnswer(returnsFirstArg());
        given(configurationBo.findOrSetDefault(any(), any())).willReturn(Configuration.builder().value("TRUE").build());
        given(obtainedUnitBo.areUnitsInvolved(conquerorUser, targetPlanet)).willReturn(true);
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());

        unitMissionBo.runUnitMission(GATHER_MISSION_ID, MissionType.GATHER);

        verify(missionRepository, times(1)).findById(GATHER_MISSION_ID);
        verify(obtainedUnitBo, times(1)).findByMissionId(GATHER_MISSION_ID);
        verify(configurationBo, times(1)).findOrSetDefault("MISSION_GATHER_TRIGGER_ATTACK", "FALSE");
        verify(obtainedUnitBo, times(1)).areUnitsInvolved(conquerorUser, targetPlanet);
        verify(attackMissionManagerBo, times(1)).buildAttackInformation(targetPlanet, mission);
        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(missionReportBo, times(1)).create(any(), eq(true), eq(oldPlanetOwner));
        verify(missionBo, times(1)).emitEnemyMissionsChange(oldPlanetOwner);
    }

    @Test
    void runUnitMission_attack_should_work_not_returning_as_mission_is_removed_and_emit_removed_missions_for_planet_owner() {
        var targetPlanet = givenTargetPlanet();
        var oldPlanetOwner = givenUser2();
        var oldPlanetOwnerAlliance = givenAlliance();
        oldPlanetOwner.setAlliance(oldPlanetOwnerAlliance);
        targetPlanet.setOwner(oldPlanetOwner);
        var conquerorUser = givenUser1();
        var mission = givenAttackMission();
        mission.setTargetPlanet(targetPlanet);
        mission.setUser(conquerorUser);
        mission.setRequiredTime(108D);
        var involvedInMission = List.of(givenObtainedUnit1());
        var attackInformation = givenFullAttackInformation();
        attackInformation.setRemoved(true);
        attackInformation.getUsersWithDeletedMissions().add(USER_ID_1);
        attackInformation.getUsers().get(USER_ID_2).getUnits().get(0).setFinalCount(0L);
        verify(planetBo, never()).isHomePlanet(any(Planet.class));
        var missionReport = givenReport();
        given(missionRepository.findById(ATTACK_MISSION_ID)).willReturn(Optional.of(mission));
        given(obtainedUnitBo.findByMissionId(ATTACK_MISSION_ID)).willReturn(involvedInMission);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission))
                .willReturn(attackInformation);
        given(missionReportBo.create(any(), anyBoolean(), any())).willReturn(missionReport);
        given(missionTypeRepository.findOneByCode(MissionType.RETURN_MISSION.name())).willReturn(Optional.of(givenMissionType(MissionType.RETURN_MISSION)));
        given(missionReportBo.save(any(MissionReport.class))).willAnswer(returnsFirstArg());
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());

        unitMissionBo.runUnitMission(ATTACK_MISSION_ID, MissionType.ATTACK);

        verify(missionRepository, times(1)).findById(ATTACK_MISSION_ID);
        verify(obtainedUnitBo, times(1)).findByMissionId(ATTACK_MISSION_ID);
        verify(attackMissionManagerBo, times(1)).buildAttackInformation(targetPlanet, mission);
        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(missionReportBo, times(1)).create(any(), eq(true), eq(oldPlanetOwner));
        verify(missionBo, times(1)).emitEnemyMissionsChange(oldPlanetOwner);
    }

    @Test
    void emitEnemyMissionsChange_should_not_emit_when_planet_owner_is_null_or_same_as_mission_to_delete() {
        var user = givenUser1();
        var targetPlanet = givenTargetPlanet();
        var mission = givenRawMission(givenSourcePlanet(), targetPlanet);

        unitMissionBo.emitEnemyMissionsChange(mission);
        targetPlanet.setOwner(user);
        mission.setUser(user);
        unitMissionBo.emitEnemyMissionsChange(mission);

        verify(missionBo, never()).emitEnemyMissionsChange(user);
    }

    @Test
    void emitEnemyMissionsChange_should_emit() {
        var user = givenUser1();
        var planetOwner = givenUser2();
        var targetPlanet = givenTargetPlanet();
        var mission = givenRawMission(givenSourcePlanet(), targetPlanet);
        mission.setUser(user);
        targetPlanet.setOwner(planetOwner);

        unitMissionBo.emitEnemyMissionsChange(mission);

        verify(missionBo, times(1)).emitEnemyMissionsChange(planetOwner);
    }
}
