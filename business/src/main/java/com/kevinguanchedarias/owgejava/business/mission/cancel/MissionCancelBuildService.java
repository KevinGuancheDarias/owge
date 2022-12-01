package com.kevinguanchedarias.owgejava.business.mission.cancel;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitModificationBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MissionCancelBuildService {
    private final UserStorageRepository userStorageRepository;
    private final ObtainedUnitModificationBo obtainedUnitModificationBo;
    private final TransactionUtilService transactionUtilService;
    private final SocketIoService socketIoService;
    private final UnitTypeBo unitTypeBo;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MissionFinderBo missionFinderBo;

    @Transactional
    public void cancel(Mission mission) {
        var missionUser = mission.getUser();
        obtainedUnitModificationBo.deleteByMissionId(mission.getId());
        missionUser.addtoPrimary(mission.getPrimaryResource());
        missionUser.addToSecondary(mission.getSecondaryResource());
        userStorageRepository.save(missionUser);
        transactionUtilService.doAfterCommit(() -> {
            socketIoService.sendMessage(missionUser, MissionBo.UNIT_BUILD_MISSION_CHANGE,
                    () -> missionFinderBo.findBuildMissions(missionUser.getId()));
            emitUser(missionUser.getId());
        });
    }

    private void emitUser(Integer userId) {
        unitTypeBo.emitUserChange(userId);
        missionEventEmitterBo.emitMissionCountChange(userId);
    }
}
