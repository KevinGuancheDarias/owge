package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

@Service
@AllArgsConstructor
public class MissionEventEmitterBo {
    public static final String ENEMY_MISSION_CHANGE = "enemy_mission_change";
    public static final String UNIT_MISSION_CHANGE = "unit_mission_change";

    private final TransactionUtilService transactionUtilService;
    private final EntityManager entityManager;
    private final SocketIoService socketIoService;
    private final RunningMissionFinderBo runningMissionFinderBo;

    public void emitLocalMissionChangeAfterCommit(Mission mission) {
        UserStorage user = mission.getUser();
        transactionUtilService.doAfterCommit(() -> emitLocalMissionChange(mission, user.getId()));
    }

    /**
     * Emits the specified mission to the <i>mission</i> target planet owner if any
     * <br>
     * As of 0.9.9 this method is now public
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    public void emitEnemyMissionsChange(Mission mission) {
        UserStorage targetPlanetOwner = mission.getTargetPlanet().getOwner();
        if (targetPlanetOwner != null && !targetPlanetOwner.getId().equals(mission.getUser().getId())) {
            emitEnemyMissionsChange(targetPlanetOwner);
        }
    }

    public void emitEnemyMissionsChange(UserStorage user) {
        socketIoService.sendMessage(user, ENEMY_MISSION_CHANGE, () -> runningMissionFinderBo.findEnemyRunningMissions(user));
    }

    public void emitUnitMissions(Integer userId) {
        socketIoService.sendMessage(userId, UNIT_MISSION_CHANGE,
                () -> MissionWebsocketMessage.builder()
                        .count(runningMissionFinderBo.countUserRunningMissions(userId))
                        .myUnitMissions(runningMissionFinderBo.findUserRunningMissions(userId))
                        .build()
        );
    }

    public void emitLocalMissionChange(Mission mission, Integer userId) {
        entityManager.refresh(mission);
        if (Boolean.FALSE.equals(mission.getInvisible())) {
            emitEnemyMissionsChange(mission);
        }
        emitUnitMissions(userId);
    }
}
