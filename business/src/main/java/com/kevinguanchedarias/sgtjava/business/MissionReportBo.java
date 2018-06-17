package com.kevinguanchedarias.sgtjava.business;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.dto.MissionReportDto;
import com.kevinguanchedarias.sgtjava.entity.MissionReport;
import com.kevinguanchedarias.sgtjava.repository.MissionReportRepository;

@Service
public class MissionReportBo implements BaseBo<MissionReport> {
	private static final long serialVersionUID = -3125120788150047385L;

	private static final Integer DEFAULT_PAGE_SIZE = 15;

	@Autowired
	private MissionReportRepository missionReportRepository;

	@Autowired
	private MissionBo missionBo;

	@Override
	public JpaRepository<MissionReport, Number> getRepository() {
		return missionReportRepository;
	}

	public List<MissionReportDto> findPaginatedByUserId(Integer userId, Integer page) {
		List<MissionReport> retVal = missionReportRepository.findByUserIdOrderByIdDesc(userId,
				new PageRequest(page, DEFAULT_PAGE_SIZE));
		return retVal.stream().map(current -> {
			MissionReportDto currentDto = new MissionReportDto();
			currentDto.dtoFromEntity(current);
			currentDto.parseMission(missionBo.findOneByReportId(current.getId()));
			return currentDto;
		}).collect(Collectors.toList());
	}

}
