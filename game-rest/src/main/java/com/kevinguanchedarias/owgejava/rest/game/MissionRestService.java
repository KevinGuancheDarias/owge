package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.business.mission.RunningMissionFinderBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("game/mission")
@ApplicationScope
@AllArgsConstructor
public class MissionRestService implements SyncSource {
    private final UnitMissionBo unitMissionBo;
    private final RunningMissionFinderBo runningMissionFinderBo;

    @PostMapping("explorePlanet")
    public void explorePlanet(@RequestBody UnitMissionInformation missionInformation) {
        unitMissionBo.myRegisterExploreMission(missionInformation);
    }

    @PostMapping("gather")
    public void gather(@RequestBody UnitMissionInformation missionInformation) {
        unitMissionBo.myRegisterGatherMission(missionInformation);
    }

    @PostMapping("establishBase")
    public void establishBase(@RequestBody UnitMissionInformation missionInformation) {
        unitMissionBo.myRegisterEstablishBaseMission(missionInformation);
    }

    @PostMapping("attack")
    public void attack(@RequestBody UnitMissionInformation missionInformation) {
        unitMissionBo.myRegisterAttackMission(missionInformation);
    }

    @PostMapping("counterattack")
    public void counterattack(@RequestBody UnitMissionInformation missionInformation) {
        unitMissionBo.myRegisterCounterattackMission(missionInformation);
    }

    @PostMapping("conquest")
    public void conquest(@RequestBody UnitMissionInformation missionInformation) {
        unitMissionBo.myRegisterConquestMission(missionInformation);
    }

    @PostMapping("deploy")
    public void deploy(@RequestBody UnitMissionInformation missionInformation) {
        unitMissionBo.myRegisterDeploy(missionInformation);
    }

    @PostMapping("cancel")
    public String cancel(@RequestParam("id") Long id) {
        unitMissionBo.myCancelMission(id);
        return "\"OK\"";
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("missions_count_change", this::findCount)
                .withHandler("unit_mission_change",
                        user -> new MissionWebsocketMessage(findCount(user),
                                runningMissionFinderBo.findUserRunningMissions(user.getId())))
                .withHandler("enemy_mission_change", runningMissionFinderBo::findEnemyRunningMissions).build();
    }

    private Integer findCount(UserStorage user) {
        return runningMissionFinderBo.countUserRunningMissions(user.getId());
    }
}
