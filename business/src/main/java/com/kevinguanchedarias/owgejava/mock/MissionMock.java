package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.MissionType;
import com.kevinguanchedarias.owgejava.entity.Planet;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;

@UtilityClass
public class MissionMock {
    public static final long DEPLOYED_MISSION_ID = 14;
    public static final long ATTACK_MISSION_ID = 144;
    public static final long CONQUEST_MISSION_ID = 2490;
    public static final long GATHER_MISSION_ID = 49281;
    public static final long EXPLORE_MISSION_ID = 1428;

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

    public static Mission givenGatherMission() {
        var mission = givenRawMission(givenSourcePlanet(), givenTargetPlanet());
        mission.setType(givenMissionType(com.kevinguanchedarias.owgejava.enumerations.MissionType.GATHER));
        return mission;
    }

    public static Mission givenRawMission(Planet sourcePlanet, Planet targetPlanet) {
        var mission = new Mission();
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
}
