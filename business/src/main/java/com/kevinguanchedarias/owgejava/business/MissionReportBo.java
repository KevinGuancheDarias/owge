package com.kevinguanchedarias.owgejava.business;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.kevinsuite.commons.exception.CommonException;
import com.kevinguanchedarias.owgejava.dto.MissionReportDto;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.MissionReportRepository;
import com.kevinguanchedarias.owgejava.responses.MissionReportResponse;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;

@Service
public class MissionReportBo implements BaseBo<Long, MissionReport, MissionReportDto> {
	private static final long serialVersionUID = -3125120788150047385L;

	private static final Integer DEFAULT_PAGE_SIZE = 15;

	@Autowired
	private MissionReportRepository missionReportRepository;

	@Autowired
	private MissionBo missionBo;

	@Autowired
	private transient SocketIoService socketIoService;

	@Autowired
	private ObjectMapper mapper;

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

	@Transactional
	@Override
	public MissionReport save(MissionReport entity) {
		MissionReport savedReport = BaseBo.super.save(entity);
		UserStorage user = entity.getUser();

		TransactionUtil.doAfterCommit(() -> {
			socketIoService.sendMessage(user, "mission_report_change",
					() -> findMissionReportsInformation(user.getId(), 0));
		});
		return savedReport;
	}

	/**
	 *
	 * @param userId
	 * @param page
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionReportResponse findMissionReportsInformation(Integer userId, Integer page) {
		MissionReportResponse missionReportResponse = new MissionReportResponse();
		missionReportResponse.setPage(page);
		missionReportResponse.setReports(findPaginatedByUserId(userId, page));
		missionReportResponse = findUnreadCount(userId, missionReportResponse);
		missionReportResponse.setReports(parseJsonBody(missionReportResponse.getReports()));
		return missionReportResponse;
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

	/**
	 *
	 * @param reports
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<MissionReportDto> parseJsonBody(List<MissionReportDto> reports) {
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

	/**
	 * Fills the unread user and unread enemy reports
	 *
	 * @param userId
	 * @param missionReportResponse
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionReportResponse findUnreadCount(Integer userId, MissionReportResponse missionReportResponse) {
		MissionReportResponse retVal = missionReportResponse == null ? new MissionReportResponse()
				: missionReportResponse;
		retVal.setEnemyUnread(missionReportRepository.countByUserIdAndIsEnemyAndUserReadDateIsNull(userId, true));
		retVal.setUserUnread(missionReportRepository.countByUserIdAndIsEnemyAndUserReadDateIsNull(userId, false));
		return retVal;
	}

	/**
	 * Marks the list of reports as read (only if the belong to the userId)
	 *
	 * @param userId
	 * @param reportsIds
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void markAsRead(Integer userId, List<Long> reportsIds) {
		missionReportRepository.markAsReadIfUserIsOwner(reportsIds, userId);
		socketIoService.sendMessage(userId, "mission_report_count_change", () -> findUnreadCount(userId, null));
	}

}
