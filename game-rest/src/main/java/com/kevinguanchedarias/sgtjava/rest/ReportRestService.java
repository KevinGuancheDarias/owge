package com.kevinguanchedarias.sgtjava.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.kevinsuite.commons.exception.CommonException;
import com.kevinguanchedarias.sgtjava.business.MissionReportBo;
import com.kevinguanchedarias.sgtjava.business.UserStorageBo;
import com.kevinguanchedarias.sgtjava.dto.MissionReportDto;

@RestController
@RequestMapping("report")
@ApplicationScope
public class ReportRestService {

	@Autowired
	private MissionReportBo missionReportBo;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObjectMapper mapper;

	@RequestMapping("findMy")
	public List<MissionReportDto> findMy(@RequestParam("page") Integer page) {
		return parseJsonBody(missionReportBo.findPaginatedByUserId(userStorageBo.findLoggedIn().getId(), page - 1));
	}

	private List<MissionReportDto> parseJsonBody(List<MissionReportDto> reports) {
		return reports.stream().map(current -> {
			try {
				if (current.getJsonBody() != null) {
					current.setParsedJson(
							mapper.readValue(current.getJsonBody(), new TypeReference<HashMap<String, Object>>() {
							}));
					current.setJsonBody(null);
				}
			} catch (IOException e) {
				throw new CommonException("Unexpected shit", e);
			}
			return current;
		}).collect(Collectors.toList());
	}
}
