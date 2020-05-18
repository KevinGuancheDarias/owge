package com.kevinguanchedarias.owgejava.business;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.MissionReportDto;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.repository.MissionReportRepository;

@Service
public class MissionReportBo implements BaseBo<Long, MissionReport, MissionReportDto> {
	private static final long serialVersionUID = -3125120788150047385L;

	private static final Integer DEFAULT_PAGE_SIZE = 15;

	@Autowired
	private MissionReportRepository missionReportRepository;

	@Autowired
	private MissionBo missionBo;

	@Override
	public JpaRepository<MissionReport, Long> getRepository() {
		return missionReportRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<MissionReportDto> getDtoClass() {
		return MissionReportDto.class;
	}

	public List<MissionReportDto> findPaginatedByUserId(Integer userId, Integer page) {
		List<MissionReport> retVal = missionReportRepository.findByUserIdOrderByIdDesc(userId,
				PageRequest.of(page, DEFAULT_PAGE_SIZE));
		return retVal.stream().map(current -> {
			MissionReportDto currentDto = new MissionReportDto();
			currentDto.dtoFromEntity(current);
			currentDto.parseMission(missionBo.findOneByReportId(current.getId()));
			return currentDto;
		}).collect(Collectors.toList());
	}

}
