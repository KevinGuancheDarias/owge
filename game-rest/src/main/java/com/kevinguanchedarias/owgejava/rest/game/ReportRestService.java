package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.responses.MissionReportResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("game/report")
@ApplicationScope
@AllArgsConstructor
public class ReportRestService implements SyncSource {
    private final MissionReportBo missionReportBo;
    private final UserSessionService userSessionService;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     */
    @Deprecated(since = "0.9.6")
    @GetMapping("findMy")
    public MissionReportResponse findMy(@RequestParam("page") Integer page) {
        return missionReportBo.findMissionReportsInformation(userSessionService.findLoggedIn().getId(), page - 1);
    }

    @PostMapping("mark-as-read")
    public void markAsRead(@RequestBody List<Long> reportsIds) {
        missionReportBo.markAsRead(userSessionService.findLoggedIn().getId(), reportsIds);
    }

    @PostMapping("mark-as-read-before-date/{date}")
    public void markAsReadBeforeDate(@PathVariable Instant date) {
        missionReportBo.markAsReadBeforeDate(userSessionService.findLoggedIn().getId(), date);
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("mission_report_change",
                user -> missionReportBo.findMissionReportsInformation(user.getId(), 0)).build();
    }

}
