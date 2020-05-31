package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.responses.MissionReportResponse;

@RestController
@RequestMapping("game/report")
@ApplicationScope
public class ReportRestService {

	@Autowired
	private MissionReportBo missionReportBo;

	@Autowired
	private UserStorageBo userStorageBo;

	@GetMapping("findMy")
	public MissionReportResponse findMy(@RequestParam("page") Integer page) {
		return missionReportBo.findMissionReportsInformation(userStorageBo.findLoggedIn().getId(), page - 1);
	}

	@PostMapping("mark-as-read")
	public void markAsRead(@RequestBody List<Long> reportsIds) {
		missionReportBo.markAsRead(userStorageBo.findLoggedIn().getId(), reportsIds);
	}

}
