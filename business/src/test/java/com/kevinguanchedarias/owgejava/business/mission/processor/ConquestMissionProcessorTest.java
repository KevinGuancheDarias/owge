package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.cancel.MissionCancelBuildService;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.GlobalConstants.MAX_PLANETS_MESSAGE;
import static com.kevinguanchedarias.owgejava.mock.AllianceMock.givenAlliance;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenFullAttackInformation;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenFullAttackInformationWithAlly;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenBuildMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenConquestMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static com.kevinguanchedarias.owgejava.mock.SpecialLocationMock.givenSpecialLocation;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = ConquestMissionProcessor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetBo.class,
        ReturnMissionRegistrationBo.class,
        AttackMissionProcessor.class,
        RequirementBo.class,
        MissionEventEmitterBo.class,
        MissionReportManagerBo.class,
        MissionCancelBuildService.class,
        MissionRepository.class
})
class ConquestMissionProcessorTest {
    private static final Planet SOURCE_PLANET = givenSourcePlanet();
    private static final UserStorage PLANET_OWNER = givenUser2().toBuilder().alliance(givenAlliance()).build();
    private static final Planet TARGET_PLANET = givenTargetPlanet();
    private static final Planet TARGET_PLANET_WITH_OWNER = TARGET_PLANET.toBuilder().owner(PLANET_OWNER).build();
    private static final UserStorage ATTACKER_USER = givenUser1();

    private final ConquestMissionProcessor conquestMissionProcessor;
    private final AttackMissionProcessor attackMissionProcessor;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final PlanetBo planetBo;
    private final RequirementBo requirementBo;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MissionReportManagerBo missionReportManagerBo;
    private final MissionCancelBuildService missionCancelBuildService;
    private final MissionRepository missionRepository;

    @Autowired
    ConquestMissionProcessorTest(
            ConquestMissionProcessor conquestMissionProcessor,
            AttackMissionProcessor attackMissionProcessor,
            ReturnMissionRegistrationBo returnMissionRegistrationBo,
            PlanetBo planetBo,
            RequirementBo requirementBo,
            MissionEventEmitterBo missionEventEmitterBo,
            MissionReportManagerBo missionReportManagerBo,
            MissionCancelBuildService missionCancelBuildService,
            MissionRepository missionRepository
    ) {
        this.conquestMissionProcessor = conquestMissionProcessor;
        this.attackMissionProcessor = attackMissionProcessor;
        this.returnMissionRegistrationBo = returnMissionRegistrationBo;
        this.planetBo = planetBo;
        this.requirementBo = requirementBo;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.missionReportManagerBo = missionReportManagerBo;
        this.missionCancelBuildService = missionCancelBuildService;
        this.missionRepository = missionRepository;
    }


    @Test
    void supports_should_work() {
        assertThat(conquestMissionProcessor.supports(MissionType.EXPLORE)).isFalse();
        assertThat(conquestMissionProcessor.supports(MissionType.CONQUEST)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("process_should_handle_conquered_not_owned_planet_arguments")
    void process_should_handle_conquered_not_owned_planet(SpecialLocation specialLocation) {
        var targetPlanet = TARGET_PLANET.toBuilder().specialLocation(specialLocation).build();
        var mission = givenConquestMission(SOURCE_PLANET, targetPlanet);
        var involvedUnits = List.of(givenObtainedUnit1());
        mission.setUser(ATTACKER_USER);
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(() -> UnitMissionReportBuilder.create(ATTACKER_USER, SOURCE_PLANET, targetPlanet, involvedUnits))
                    .thenReturn(reportBuilderMock);

            var retVal = conquestMissionProcessor.process(mission, involvedUnits);

            verify(planetBo, times(1)).hasMaxPlanets(ATTACKER_USER);
            verify(attackMissionProcessor, times(1)).processAttack(mission, false, false);
            verify(planetBo, times(1)).definePlanetAsOwnedBy(ATTACKER_USER, involvedUnits, targetPlanet);
            verify(requirementBo, never()).triggerSpecialLocation(any(), any());
            verify(planetBo, never()).emitPlanetOwnedChange(any(UserStorage.class));
            verify(missionEventEmitterBo, never()).emitEnemyMissionsChange(any(UserStorage.class));
            verify(missionEventEmitterBo, times(1)).emitLocalMissionChangeAfterCommit(mission);
            assertThat(mission.getResolved()).isTrue();
            assertThat(retVal).isEqualTo(reportBuilderMock);
        }
    }

    @ParameterizedTest
    @MethodSource("process_should_handle_failed_conquest_arguments")
    void process_should_handle_failed_conquest(
            AttackInformation attackInformation,
            boolean isAttackRemoved,
            int timesRegisterReturnMission,
            boolean isHomePlanet,
            boolean hasMaxPlanets,
            Alliance ownerAlliance,
            String wantedMessage
    ) {
        attackInformation.setRemoved(isAttackRemoved);
        var targetPlanet = TARGET_PLANET_WITH_OWNER.toBuilder()
                .owner(PLANET_OWNER.toBuilder().alliance(ownerAlliance).build())
                .build();
        var mission = givenConquestMission(SOURCE_PLANET, targetPlanet);
        var involvedUnits = List.of(givenObtainedUnit1());
        mission.setUser(ATTACKER_USER);
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        given(attackMissionProcessor.processAttack(mission, false, false)).willReturn(attackInformation);
        given(planetBo.isHomePlanet(targetPlanet)).willReturn(isHomePlanet);
        given(planetBo.hasMaxPlanets(ATTACKER_USER)).willReturn(hasMaxPlanets);
        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(() -> UnitMissionReportBuilder.create(ATTACKER_USER, SOURCE_PLANET, TARGET_PLANET, involvedUnits))
                    .thenReturn(reportBuilderMock);

            var retVal = conquestMissionProcessor.process(mission, involvedUnits);

            verify(returnMissionRegistrationBo, times(timesRegisterReturnMission)).registerReturnMission(mission, null);
            verify(reportBuilderMock, times(1)).withConquestInformation(false, wantedMessage);
            assertThat(mission.getResolved()).isTrue();
            assertThat(retVal).isEqualTo(reportBuilderMock);
        }
    }

    @ParameterizedTest
    @MethodSource("process_should_handle_success_conquer_arguments")
    void process_should_handle_success_conquer(
            SpecialLocation specialLocation,
            int triggerRequirementsForOldOwner,
            Mission runningBuildMission,
            int timesCancelBuildMission
    ) {
        var targetPlanet = TARGET_PLANET_WITH_OWNER.toBuilder().specialLocation(specialLocation).build();
        var mission = givenConquestMission(SOURCE_PLANET, targetPlanet);
        var involvedUnits = List.of(givenObtainedUnit1());
        mission.setUser(ATTACKER_USER);
        var defeatedOwnerAttackInformation = givenFullAttackInformation();
        defeatedOwnerAttackInformation.getUsers().get(USER_ID_2).getUnits().get(0).setFinalCount(0L);
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        var reportForOwnerMock = mock(UnitMissionReportBuilder.class);
        given(attackMissionProcessor.processAttack(mission, false, false)).willReturn(defeatedOwnerAttackInformation);
        given(reportForOwnerMock.withConquestInformation(any(), any())).willReturn(reportForOwnerMock);
        given(missionRepository.findOneByResolvedFalseAndTypeCodeAndMissionInformationValue(
                MissionType.BUILD_UNIT.name(), (double) TARGET_PLANET_ID
        )).willReturn(Optional.ofNullable(runningBuildMission));

        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(() -> UnitMissionReportBuilder.create(ATTACKER_USER, SOURCE_PLANET, targetPlanet, involvedUnits))
                    .thenReturn(reportBuilderMock, reportForOwnerMock);
            var retVal = conquestMissionProcessor.process(mission, involvedUnits);

            verify(planetBo, times(1)).definePlanetAsOwnedBy(ATTACKER_USER, involvedUnits, targetPlanet);
            verify(requirementBo, times(triggerRequirementsForOldOwner)).triggerSpecialLocation(PLANET_OWNER, specialLocation);
            verify(missionCancelBuildService, times(timesCancelBuildMission)).cancel(runningBuildMission);
            verify(missionEventEmitterBo, times(1)).emitEnemyMissionsChange(PLANET_OWNER);
            verify(reportForOwnerMock, times(1)).withConquestInformation(true, "I18N_YOUR_PLANET_WAS_CONQUISTED");
            verify(missionReportManagerBo, times(1)).handleMissionReportSave(mission, reportForOwnerMock, true, PLANET_OWNER);
            assertThat(retVal).isEqualTo(reportBuilderMock);
        }
    }

    private static Stream<Arguments> process_should_handle_conquered_not_owned_planet_arguments() {
        return Stream.of(
                Arguments.of(givenSpecialLocation()),
                Arguments.of((Object) null)
        );
    }

    private static Stream<Arguments> process_should_handle_failed_conquest_arguments() {
        var ownedNotDefeatedAttackInformation = givenFullAttackInformation();
        var allianceNotDefeatedAttackInformation = givenFullAttackInformationWithAlly();
        var allianceNotDefeatedButAttackerHasAllianceToo = givenFullAttackInformationWithAlly();
        allianceNotDefeatedButAttackerHasAllianceToo.getUsers().get(USER_ID_1).getUser().setAlliance(givenAlliance(2373373));
        var defeatedOwnerAttackInformation = givenFullAttackInformation();
        defeatedOwnerAttackInformation.getUsers().get(USER_ID_2).getUnits().get(0).setFinalCount(0L);
        var ownerNotPresentInPlanetAttackInformation = givenFullAttackInformationWithAlly();
        ownerNotPresentInPlanetAttackInformation.getUsers().remove(USER_ID_2);
        var ownerAlliance = PLANET_OWNER.getAlliance();
        return Stream.of(
                Arguments.of(ownedNotDefeatedAttackInformation, true, 0, false, false, ownerAlliance, "I18N_OWNER_NOT_DEFEATED"),
                Arguments.of(ownedNotDefeatedAttackInformation, false, 1, false, false, ownerAlliance, "I18N_OWNER_NOT_DEFEATED"),
                Arguments.of(allianceNotDefeatedAttackInformation, true, 0, false, false, ownerAlliance, "I18N_ALLIANCE_NOT_DEFEATED"),
                Arguments.of(allianceNotDefeatedAttackInformation, false, 1, false, false, ownerAlliance, "I18N_ALLIANCE_NOT_DEFEATED"),
                Arguments.of(ownerNotPresentInPlanetAttackInformation, false, 1, false, false, ownerAlliance, "I18N_ALLIANCE_NOT_DEFEATED"),
                Arguments.of(allianceNotDefeatedButAttackerHasAllianceToo, true, 0, false, false, ownerAlliance, "I18N_ALLIANCE_NOT_DEFEATED"),
                Arguments.of(allianceNotDefeatedButAttackerHasAllianceToo, false, 1, false, false, ownerAlliance, "I18N_ALLIANCE_NOT_DEFEATED"),
                Arguments.of(defeatedOwnerAttackInformation, true, 0, false, true, ownerAlliance, MAX_PLANETS_MESSAGE),
                Arguments.of(defeatedOwnerAttackInformation, true, 0, false, true, null, MAX_PLANETS_MESSAGE),
                Arguments.of(defeatedOwnerAttackInformation, true, 0, true, false, ownerAlliance, "I18N_CANT_CONQUER_HOME_PLANET")
        );
    }

    private static Stream<Arguments> process_should_handle_success_conquer_arguments() {
        var specialLocation = givenSpecialLocation();
        var buildMission = givenBuildMission();
        return Stream.of(
                Arguments.of(null, 0, null, 0),
                Arguments.of(specialLocation, 1, null, 0),
                Arguments.of(specialLocation, 1, buildMission, 1),
                Arguments.of(null, 0, buildMission, 1)
        );
    }
}
