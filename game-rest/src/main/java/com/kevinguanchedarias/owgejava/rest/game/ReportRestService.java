package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.responses.MissionReportResponse;

@RestController
@RequestMapping("game/report")
@ApplicationScope
public class ReportRestService implements SyncSource {

	@Autowired
	private MissionReportBo missionReportBo;

	@Autowired
	private UserStorageBo userStorageBo;

	/**
	 *
	 * @param page
	 * @return
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.9.6")
	@GetMapping("findMy")
	public MissionReportResponse findMy(@RequestParam("page") Integer page) {
		return missionReportBo.findMissionReportsInformation(userStorageBo.findLoggedIn().getId(), page - 1);
	}

	@PostMapping("mark-as-read")
	public void markAsRead(@RequestBody List<Long> reportsIds) {
		missionReportBo.markAsRead(userStorageBo.findLoggedIn().getId(), reportsIds);
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("mission_report_change",
				user -> missionReportBo.findMissionReportsInformation(user.getId(), 0)).build();
	}

}
