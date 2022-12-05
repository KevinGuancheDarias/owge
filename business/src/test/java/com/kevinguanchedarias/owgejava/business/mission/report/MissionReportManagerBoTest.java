package com.kevinguanchedarias.owgejava.business.mission.report;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.REPORT_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.givenReport;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MissionReportManagerBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(MissionReportBo.class)
class MissionReportManagerBoTest {
    private final MissionReportManagerBo missionReportManagerBo;
    private final MissionReportBo missionReportBo;

    @Autowired
    MissionReportManagerBoTest(MissionReportManagerBo missionReportManagerBo, MissionReportBo missionReportBo) {
        this.missionReportManagerBo = missionReportManagerBo;
        this.missionReportBo = missionReportBo;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handleMissionReportSave_should_work(boolean isEnemy) {
        var mission = givenExploreMission();
        var user1 = givenUser1();
        var user2 = givenUser2();
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        var missionReport = givenReport();
        given(missionReportBo.create(reportBuilderMock, isEnemy, user1)).willReturn(missionReport);
        given(missionReportBo.create(reportBuilderMock, isEnemy, user2)).willReturn(missionReport);

        missionReportManagerBo.handleMissionReportSave(mission, reportBuilderMock, isEnemy, List.of(user1, user2));
        verify(missionReportBo, times(1)).create(reportBuilderMock, isEnemy, user1);
        verify(missionReportBo, times(1)).create(reportBuilderMock, isEnemy, user2);
        assertThat(missionReport.getMission()).isEqualTo(mission);
        assertThat(mission.getReport()).isEqualTo(missionReport);
    }

    @Test
    void handleMissionReportSave_regular_should_work() {
        var mission = givenExploreMission();
        var user = givenUser1();
        var json = "foo_bar_baz";
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        var reportEntity = givenReport().toBuilder().isEnemy(null).build();
        mission.setUser(user);
        given(missionReportBo.save(any(MissionReport.class))).willReturn(reportEntity);
        given(reportBuilderMock.withId(REPORT_ID)).willReturn(reportBuilderMock);
        given(reportBuilderMock.buildJson()).willReturn(json);

        missionReportManagerBo.handleMissionReportSave(mission, reportBuilderMock);

        var passedReportCaptor = ArgumentCaptor.forClass(MissionReport.class);
        verify(missionReportBo, times(1)).save(passedReportCaptor.capture());
        assertThat(passedReportCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(reportEntity.getJsonBody()).isEqualTo(json);
        assertThat(reportEntity.getIsEnemy()).isFalse();
        assertThat(mission.getReport()).isEqualTo(reportEntity);
    }
}
