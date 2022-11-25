package com.kevinguanchedarias.owgejava.business.mission.report;


import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class MissionReportManagerBo {
    private final MissionReportBo missionReportBo;

    public void handleMissionReportSave(Mission mission, UnitMissionReportBuilder builder, boolean isEnemy,
                                        List<UserStorage> users) {
        users.forEach(currentUser -> handleMissionReportSave(mission, builder, isEnemy, currentUser));
    }

    public void handleMissionReportSave(Mission mission, UnitMissionReportBuilder builder, boolean isEnemy,
                                        UserStorage user) {
        MissionReport missionReport = missionReportBo.create(builder, isEnemy, user);
        missionReport.setMission(mission);
        mission.setReport(missionReport);
    }

    /**
     * Saves the MissionReport to the database
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void handleMissionReportSave(Mission mission, UnitMissionReportBuilder builder) {
        MissionReport missionReport = new MissionReport("{}", mission);
        missionReport.setUser(mission.getUser());
        missionReport = missionReportBo.save(missionReport);
        missionReport.setReportDate(new Date());
        missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
        missionReport.setIsEnemy(false);
        mission.setReport(missionReport);
    }
}
