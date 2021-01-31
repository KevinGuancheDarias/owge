package com.kevinguanchedarias.owgejava.business;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.kevinsuite.commons.exception.CommonException;
import com.kevinguanchedarias.owgejava.dto.MissionReportDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.MissionReportRepository;
import com.kevinguanchedarias.owgejava.responses.MissionReportResponse;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;

@Service
public class MissionReportBo implements BaseBo<Long, MissionReport, MissionReportDto> {
	private static final long serialVersionUID = -3125120788150047385L;

	private static final Integer DEFAULT_PAGE_SIZE = 15;
	private static final int DAYS_TO_PRESERVE_MESSAGES = 15;

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

		TransactionUtil.doAfterCommit(() -> emitOneToUser(savedReport, user));
		return savedReport;
	}

	/**
	 *
	 * @param report
	 * @param user
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void emitOneToUser(MissionReport report, UserStorage user) {
		socketIoService.sendMessage(user, "mission_report_new", () -> toDto(report));
		emitCountChange(user.getId());
	}

	public void emitToUser(UserStorage user) {
		socketIoService.sendMessage(user, "mission_report_change",
				() -> findMissionReportsInformation(user.getId(), 0));
	}

	/**
	 *
	 *
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@EventListener(ApplicationReadyEvent.class)
	@Scheduled(cron = "0 0 1 * * *")
	@Transactional
	public void deleteOldMessages() {
		HashSet<UserStorage> affectedUsers = new HashSet<>();
		missionReportRepository
				.findByReportDateLessThan(new Date(new Date().getTime() - (86400 * 1000 * DAYS_TO_PRESERVE_MESSAGES)))
				.forEach(report -> {
					Mission mission = report.getMission();
					if (mission != null) {
						mission.setReport(null);
						missionBo.save(mission);
					}
					affectedUsers.add(report.getUser());
					delete(report);
				});
		CompletableFuture.delayedExecutor(15, TimeUnit.SECONDS).execute(() -> affectedUsers.forEach(this::emitToUser));
	}

	/**
	 * Notice, if page is zero, will mark as flush required
	 *
	 * @param userId
	 * @param page
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionReportResponse findMissionReportsInformation(Integer userId, int page) {
		MissionReportResponse missionReportResponse = new MissionReportResponse();
		missionReportResponse.setPage(page);
		missionReportResponse.setReports(findPaginatedByUserId(userId, page));
		missionReportResponse = findUnreadCount(userId, missionReportResponse);
		missionReportResponse.setReports(parseJsonBody(missionReportResponse.getReports()));
		missionReportResponse.setRequiresFlush(page == 0);
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
		return reports.stream().map(this::parseJsonBody).collect(Collectors.toList());
	}

	/**
	 *
	 * @param report
	 * @return
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionReportDto parseJsonBody(MissionReportDto report) {
		try {
			if (report.getJsonBody() != null) {
				report.setParsedJson(
						mapper.readValue(report.getJsonBody(), new TypeReference<HashMap<String, Object>>() {
						}));
				report.setJsonBody(null);
			}
		} catch (IOException e) {
			throw new CommonException("Unexpected shit", e);
		}
		return report;
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
		emitCountChange(userId);
	}

	@Override
	public MissionReportDto toDto(MissionReport entity) {
		return parseJsonBody(BaseBo.super.toDto(entity));
	}

	private void emitCountChange(Integer userId) {
		socketIoService.sendMessage(userId, "mission_report_count_change", () -> findUnreadCount(userId, null));
	}

}
