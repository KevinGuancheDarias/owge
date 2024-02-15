package com.kevinguanchedarias.owgejava.business;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.MissionReportDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.repository.MissionReportRepository;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.responses.MissionReportResponse;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serial;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MissionReportBo implements BaseBo<Long, MissionReport, MissionReportDto>, UserDeleteListener {
    @Serial
    private static final long serialVersionUID = -3125120788150047385L;

    public static final String EMIT_NEW = "mission_report_new";
    public static final String EMIT_COUNT_CHANGE = "mission_report_count_change";

    private static final Integer DEFAULT_PAGE_SIZE = 15;
    private static final int DAYS_TO_PRESERVE_MESSAGES = 15;

    private final MissionReportRepository missionReportRepository;
    private final transient SocketIoService socketIoService;
    private final transient TransactionUtilService transactionUtilService;
    private final ObjectMapper mapper;
    private final MissionRepository missionRepository;

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
    public MissionReport save(MissionReport entity) {
        MissionReport savedReport = missionReportRepository.save(entity);
        UserStorage user = entity.getUser();

        transactionUtilService.doAfterCommit(() -> emitOneToUser(savedReport, user));
        return savedReport;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     */
    public void emitOneToUser(MissionReport report, UserStorage user) {
        socketIoService.sendMessage(user, EMIT_NEW, () -> toDto(report));
        emitCountChange(user.getId());
    }

    public void emitToUser(Integer userId) {
        socketIoService.sendMessage(userId, "mission_report_change", () -> findMissionReportsInformation(userId, 0));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void deleteOldMessages() {
        HashSet<Integer> affectedUsers = new HashSet<>();
        int page = 0;
        List<MissionReport> reports;
        do {
            reports = missionReportRepository.findByReportDateLessThan(
                    new Date(new Date().getTime() - (86400 * 1000 * DAYS_TO_PRESERVE_MESSAGES)),
                    PageRequest.of(page, 50));
            reports.forEach(report -> {
                Mission mission = report.getMission();
                if (mission != null) {
                    mission.setReport(null);
                    missionRepository.save(mission);
                }
                affectedUsers.add(report.getUser().getId());
                missionReportRepository.delete(report);
            });
            page++;
        } while (!reports.isEmpty());
        CompletableFuture.delayedExecutor(15, TimeUnit.SECONDS).execute(() -> affectedUsers.forEach(this::emitToUser));
    }

    /**
     * Notice, if page is zero, will mark as flush required
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
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
            currentDto.parseMission(missionRepository.findOneByReportId(current.getId()));
            return currentDto;
        }).toList();
    }

    @Transactional
    public MissionReport create(UnitMissionReportBuilder builder, boolean isEnemy,
                                UserStorage user) {
        MissionReport missionReport = new MissionReport();
        missionReport.setUser(user);
        missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
        missionReport.setReportDate(new Date());
        missionReport.setIsEnemy(isEnemy);
        missionReport = save(missionReport);
        return missionReport;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<MissionReportDto> parseJsonBody(List<MissionReportDto> reports) {
        return reports.stream().map(this::parseJsonBody).collect(Collectors.toList());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional
    public void markAsRead(Integer userId, List<Long> reportsIds) {
        missionReportRepository.markAsReadIfUserIsOwner(reportsIds, userId);
        emitCountChange(userId);
    }

    @Transactional
    public void markAsReadBeforeDate(Integer userId, Instant date) {
        missionReportRepository.markAsReadBeforeDate(userId, Date.from(date));
        emitCountChange(userId);
    }

    @Override
    public MissionReportDto toDto(MissionReport entity) {
        return parseJsonBody(BaseBo.super.toDto(entity));
    }

    @Override
    public int order() {
        return MissionBo.MISSION_USER_DELETE_ORDER - 1;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        missionRepository.updateReportId(null);
        missionReportRepository.deleteByUser(user);
    }

    private void emitCountChange(Integer userId) {
        socketIoService.sendMessage(userId, EMIT_COUNT_CHANGE, () -> findUnreadCount(userId, null));
    }
}
