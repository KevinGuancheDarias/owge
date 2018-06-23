package com.kevinguanchedarias.sgtjava.dto;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.BeanUtils;

import com.kevinguanchedarias.sgtjava.entity.Mission.MissionIdAndTerminationDateProjection;
import com.kevinguanchedarias.sgtjava.entity.MissionReport;

public class MissionReportDto implements DtoFromEntity<MissionReport> {
	private Long id;
	private String jsonBody;
	private Map<String, Object> parsedJson;
	private Long missionId;
	private Date missionDate;
	private Date reportDate;
	private Date userAwareDate;
	private Date userReadDate;

	@Override
	public void dtoFromEntity(MissionReport entity) {
		BeanUtils.copyProperties(entity, this);
	}

	public void parseMission(MissionIdAndTerminationDateProjection missionIdAndTerminationDateProjection) {
		if (missionIdAndTerminationDateProjection != null) {
			missionId = missionIdAndTerminationDateProjection.getId();
			missionDate = missionIdAndTerminationDateProjection.getDate();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getJsonBody() {
		return jsonBody;
	}

	public void setJsonBody(String jsonBody) {
		this.jsonBody = jsonBody;
	}

	public Map<String, Object> getParsedJson() {
		return parsedJson;
	}

	public void setParsedJson(Map<String, Object> parsedJson) {
		this.parsedJson = parsedJson;
	}

	public Long getMissionId() {
		return missionId;
	}

	public void setMissionId(Long missionId) {
		this.missionId = missionId;
	}

	public Date getMissionDate() {
		return missionDate;
	}

	public void setMissionDate(Date missionDate) {
		this.missionDate = missionDate;
	}

	public Date getReportDate() {
		return reportDate;
	}

	public void setReportDate(Date reportDate) {
		this.reportDate = reportDate;
	}

	public Date getUserAwareDate() {
		return userAwareDate;
	}

	public void setUserAwareDate(Date userAwareDate) {
		this.userAwareDate = userAwareDate;
	}

	public Date getUserReadDate() {
		return userReadDate;
	}

	public void setUserReadDate(Date userReadDate) {
		this.userReadDate = userReadDate;
	}

}
