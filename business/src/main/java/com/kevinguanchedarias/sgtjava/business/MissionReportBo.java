package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.entity.MissionReport;
import com.kevinguanchedarias.sgtjava.repository.MissionReportRepository;

@Service
public class MissionReportBo implements BaseBo<MissionReport> {
	private static final long serialVersionUID = -3125120788150047385L;

	@Autowired
	private MissionReportRepository missionReportRepository;

	@Override
	public JpaRepository<MissionReport, Number> getRepository() {
		return missionReportRepository;
	}

}
