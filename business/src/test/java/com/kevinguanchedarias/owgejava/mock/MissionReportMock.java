package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.MissionReport;
import lombok.experimental.UtilityClass;

import java.util.Date;

@UtilityClass
public class MissionReportMock {
    public static final long REPORT_ID = 4;
    public static final String REPORT_BODY = "FOO";
    public static final Date REPORT_DATE = new Date(29782736);
    public static final Date REPORT_USER_READ_DATE = new Date(302938282);
    public static final boolean REPORT_IS_ENEMY = false;

    public static MissionReport givenReport() {
        return MissionReport.builder()
                .id(REPORT_ID)
                .jsonBody(REPORT_BODY)
                .reportDate(REPORT_DATE)
                .userReadDate(REPORT_USER_READ_DATE)
                .isEnemy(REPORT_IS_ENEMY)
                .build();

    }
}
