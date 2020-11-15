package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;

/**
 * Has system wide actions
 *
 * @since 0.9.8
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("admin/system")
@ApplicationScope
public class AdminSystemRestService {

	@Autowired
	private SocketIoService socketIoService;

	@Autowired
	private MissionBo missionBo;

	@Autowired
	private UnitMissionBo unitMissionBo;

	/**
	 *
	 *
	 * @since 0.9.8
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("notify-updated-version")
	public void notifyUpdatedVersion() {
		socketIoService.sendMessage(0, "frontend_version_change", () -> "check for changes!!!!");
	}

	/**
	 *
	 *
	 * @since 0.9.9
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("run-hang-missions")
	public void runHangMissions() {
		missionBo.findHangMissions().forEach(mission -> {
			MissionType missionType = MissionType.valueOf(mission.getType().getCode());
			switch (missionType) {
			case LEVEL_UP:
			case BUILD_UNIT:
				missionBo.runMission(mission.getId(), missionType);
				break;
			default:
				unitMissionBo.runUnitMission(mission.getId(), missionType);
			}
		});
	}
}
