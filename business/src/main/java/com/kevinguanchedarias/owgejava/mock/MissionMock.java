package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Planet;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;

@UtilityClass
public class MissionMock {
    public static final long DEPLOYED_MISSION_ID = 14;
    public static final long ATTACK_MISSION_ID = 144;

    public static Mission givenDeployedMission() {
        var mission = new Mission();
        mission.setId(DEPLOYED_MISSION_ID);
        mission.setUser(givenUser1());
        return mission;
    }

    public static Mission givenAttackMission() {
        var mission = new Mission();
        mission.setId(ATTACK_MISSION_ID);
        mission.setUser(givenUser1());
        mission.setSourcePlanet(givenSourcePlanet());
        mission.setTargetPlanet(givenTargetPlanet());
        return mission;
    }

    public static Mission givenRawMission(Planet sourcePlanet, Planet targetPlanet) {
        var mission = new Mission();
        mission.setSourcePlanet(sourcePlanet);
        mission.setTargetPlanet(targetPlanet);
        return mission;
    }
}
