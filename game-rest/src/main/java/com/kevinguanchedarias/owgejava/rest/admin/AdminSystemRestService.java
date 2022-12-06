package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Has system-wide actions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.8
 */
@RestController
@RequestMapping("admin/system")
@ApplicationScope
@AllArgsConstructor
public class AdminSystemRestService {
    private final SocketIoService socketIoService;
    private final MissionBo missionBo;
    private final UnitMissionBo unitMissionBo;
    private final MissionRepository missionRepository;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.8
     */
    @PostMapping("notify-updated-version")
    public void notifyUpdatedVersion() {
        socketIoService.sendMessage(0, "frontend_version_change", () -> "check for changes!!!!");
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    @PostMapping("run-hang-missions")
    public void runHangMissions() {
        missionRepository.findHangMissions(LocalDateTime.now(ZoneOffset.UTC)).forEach(mission -> {
            MissionType missionType = MissionType.valueOf(mission.getType().getCode());
            var missionId = mission.getId();
            if (missionType == MissionType.BUILD_UNIT || missionType == MissionType.LEVEL_UP) {
                missionBo.runMission(missionId, missionType);
            } else {
                unitMissionBo.runUnitMission(missionId, missionType);
            }
        });
    }
}
