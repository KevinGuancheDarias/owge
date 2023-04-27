package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.*;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;

@UtilityClass
public class MissionMock {
    public static final long BUILD_MISSION_ID = 82;
    public static final double BUILD_MISSION_VALUE = SOURCE_PLANET_ID;
    public static final long DEPLOYED_MISSION_ID = 14;
    public static final long ATTACK_MISSION_ID = 144;
    public static final long CONQUEST_MISSION_ID = 2490;
    public static final long GATHER_MISSION_ID = 49281;
    public static final long EXPLORE_MISSION_ID = 1428;
    public static final long RETURN_MISSION_ID = 817271;
    public static final long ESTABLISH_BASE_MISSION_ID = 1836234;
    public static final long DEPLOY_MISSION_ID = 1899;
    public static final long UPGRADE_MISSION_ID = 11832;
    public static final double UPGRADE_MISSION_LEVEL = 7;

    public static final double MISSION_PR = 220D;
    public static final double MISSION_SR = 190D;

    public static Mission givenBuildMission() {
        var mission = new Mission();
        mission.setId(BUILD_MISSION_ID);
        mission.setSourcePlanet(givenSourcePlanet());
        mission.setTargetPlanet(givenTargetPlanet());
        mission.setUser(givenUser1());
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.BUILD_UNIT));
        mission.setMissionInformation(
                MissionInformation.builder()
                        .mission(mission)
                        .value(BUILD_MISSION_VALUE)
                        .relation(ObjectRelationMock.givenObjectRelation())
                        .build()
        );
        return mission;
    }

    public static Mission givenDeployedMission() {
        var mission = new Mission();
        mission.setId(DEPLOYED_MISSION_ID);
        mission.setUser(givenUser1());
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.DEPLOYED));
        return mission;
    }

    public static Mission givenAttackMission() {
        var mission = new Mission();
        mission.setId(ATTACK_MISSION_ID);
        mission.setUser(givenUser1());
        mission.setSourcePlanet(givenSourcePlanet());
        mission.setTargetPlanet(givenTargetPlanet());
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.ATTACK));
        return mission;
    }

    public static Mission givenExploreMission() {
        var mission = givenRawMission(givenSourcePlanet(), givenTargetPlanet());
        mission.setId(EXPLORE_MISSION_ID);
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.EXPLORE));
        return mission;
    }

    public static Mission givenEstablishBaseMission() {
        var mission = givenRawMission(givenSourcePlanet(), givenTargetPlanet());
        mission.setId(ESTABLISH_BASE_MISSION_ID);
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.ESTABLISH_BASE));
        return mission;
    }

    public static Mission givenGatherMission() {
        var mission = givenRawMission(givenSourcePlanet(), givenTargetPlanet());
        mission.setId(GATHER_MISSION_ID);
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.GATHER));
        return mission;
    }

    public static Mission givenRawMission(Planet sourcePlanet, Planet targetPlanet) {
        return givenRawMission(sourcePlanet, targetPlanet, null);
    }

    public static Mission givenRawMission(Planet sourcePlanet, Planet targetPlanet, Long id) {
        var mission = new Mission();
        mission.setId(id);
        mission.setSourcePlanet(sourcePlanet);
        mission.setTargetPlanet(targetPlanet);
        return mission;
    }

    public static MissionType givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType missionType) {
        return MissionType.builder()
                .id(missionType.getValue())
                .code(missionType.name())
                .build();
    }

    public static Mission givenConquestMission(Planet sourcePlanet, Planet targetPlanet) {
        var mission = givenRawMission(sourcePlanet, targetPlanet);
        mission.setId(CONQUEST_MISSION_ID);
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.CONQUEST));
        return mission;
    }

    public static Mission givenDeployMission() {
        var mission = givenRawMission(givenSourcePlanet(), givenTargetPlanet());
        mission.setId(DEPLOY_MISSION_ID);
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.DEPLOY));
        return mission;
    }

    public static Mission givenReturnMission() {
        var mission = givenRawMission(givenSourcePlanet(), givenTargetPlanet());
        mission.setId(RETURN_MISSION_ID);
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.RETURN_MISSION));
        return mission;
    }

    public static Mission givenUpgradeMission(ObjectRelation objectRelation) {
        var missionInformation = MissionInformation.builder()
                .relation(objectRelation)
                .value(UPGRADE_MISSION_LEVEL)
                .build();
        return Mission.builder()
                .id(UPGRADE_MISSION_ID)
                .primaryResource(MISSION_PR)
                .secondaryResource(MISSION_SR)
                .missionInformation(missionInformation)
                .type(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.LEVEL_UP))
                .build();
    }
}
