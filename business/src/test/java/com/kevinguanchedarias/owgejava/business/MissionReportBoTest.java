package com.kevinguanchedarias.owgejava.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.MissionReportDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.repository.MissionReportRepository;
import com.kevinguanchedarias.owgejava.responses.MissionReportResponse;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.kevinguanchedarias.owgejava.business.MissionReportBo.EMIT_COUNT_CHANGE;
import static com.kevinguanchedarias.owgejava.business.MissionReportBo.EMIT_NEW;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.REPORT_BODY;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.REPORT_DATE;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.REPORT_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.REPORT_IS_ENEMY;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.REPORT_USER_READ_DATE;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.givenReport;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = MissionReportBo.class
)
@MockBean({
        MissionReportRepository.class,
        MissionBo.class,
        SocketIoService.class,
        ObjectMapper.class,
        TransactionUtilService.class,
        TaggableCacheManager.class
})
class MissionReportBoTest extends AbstractBaseBoTest {
    private final MissionReportBo missionReportBo;
    private final MissionReportRepository missionReportRepository;
    private final MissionBo missionBo;
    private final TransactionUtilService transactionUtilService;
    private final SocketIoService socketIoService;
    private final ObjectMapper mapper;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public MissionReportBoTest(
            MissionReportBo missionReportBo,
            MissionReportRepository missionReportRepository,
            MissionBo missionBo,
            TransactionUtilService transactionUtilService,
            SocketIoService socketIoService,
            ObjectMapper mapper,
            TaggableCacheManager taggableCacheManager
    ) {
        this.missionReportBo = missionReportBo;
        this.missionReportRepository = missionReportRepository;
        this.missionBo = missionBo;
        this.transactionUtilService = transactionUtilService;
        this.socketIoService = socketIoService;
        this.mapper = mapper;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Test
    void findPaginatedByUserId_should_work() {
        var page = 19;
        var report = givenReport();
        var missionId = 19L;
        var missionDate = new Date(2938283);
        var reportMissionIdAndDateMock = mock(Mission.MissionIdAndTerminationDateProjection.class);
        given(missionReportRepository.findByUserIdOrderByIdDesc(eq(USER_ID_1), any()))
                .willReturn(List.of(report));
        given(reportMissionIdAndDateMock.getId()).willReturn(missionId);
        given(reportMissionIdAndDateMock.getDate()).willReturn(missionDate);
        given(missionBo.findOneByReportId(REPORT_ID)).willReturn(reportMissionIdAndDateMock);


        var result = missionReportBo.findPaginatedByUserId(USER_ID_1, page);

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(missionReportRepository, times(1)).findByUserIdOrderByIdDesc(eq(USER_ID_1), captor.capture());
        var passedPageable = captor.getValue();
        assertThat(passedPageable.getPageNumber()).isEqualTo(page);
        assertThat(passedPageable.getPageSize()).isEqualTo(15);
        assertThat(result).hasSize(1);
        var resultReport = result.get(0);
        assertThat(resultReport.getId()).isEqualTo(REPORT_ID);
        assertThat(resultReport.getJsonBody()).isEqualTo(REPORT_BODY);
        assertThat(resultReport.getReportDate()).isEqualTo(REPORT_DATE);
        assertThat(resultReport.getUserReadDate()).isEqualTo(REPORT_USER_READ_DATE);
        assertThat(resultReport.getIsEnemy()).isEqualTo(REPORT_IS_ENEMY);
        assertThat(resultReport.getMissionId()).isEqualTo(missionId);
        assertThat(resultReport.getMissionDate()).isEqualTo(missionDate);
    }

    @Test
    void create_should_work() throws JsonProcessingException {
        var user = givenUser1();
        var builderMock = mock(UnitMissionReportBuilder.class);
        var isEnemy = true;
        var json = "{}";
        var emitNewResultAnswer = new InvokeSupplierLambdaAnswer<MissionReportDto>(2);
        var emitCountChangeAnswer = new InvokeSupplierLambdaAnswer<MissionReportResponse>(2);
        var enemyUnread = 9L;
        var userUnread = 14L;
        var parsedJson = Map.of("k", "v");
        given(builderMock.withId(any())).willReturn(builderMock);
        given(builderMock.buildJson()).willReturn(json);
        doAnswer(returnsFirstArg()).when(missionReportRepository).save(any(MissionReport.class));
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        doAnswer(emitNewResultAnswer).when(socketIoService)
                .sendMessage(eq(user), eq(EMIT_NEW), any());
        doAnswer(emitCountChangeAnswer).when(socketIoService)
                .sendMessage(eq(USER_ID_1), eq(EMIT_COUNT_CHANGE), any());
        //noinspection unchecked
        given(mapper.readValue(eq(json), any(TypeReference.class))).willReturn(parsedJson);
        given(missionReportRepository.countByUserIdAndIsEnemyAndUserReadDateIsNull(USER_ID_1, true))
                .willReturn(enemyUnread);
        given(missionReportRepository.countByUserIdAndIsEnemyAndUserReadDateIsNull(USER_ID_1, false))
                .willReturn(userUnread);

        var created = missionReportBo.create(builderMock, isEnemy, user);
        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getJsonBody()).isEqualTo(json);
        assertThat(created.getReportDate()).isNotNull();
        assertThat(created.getIsEnemy()).isEqualTo(isEnemy);
        verify(missionReportRepository, times(1)).save(created);
        verify(transactionUtilService, times(1)).doAfterCommit(any());
        verify(socketIoService, times(1)).sendMessage(eq(user), eq(EMIT_NEW), any());
        var emittedReport = emitNewResultAnswer.getResult();
        assertThat(emittedReport.getParsedJson()).isEqualTo(parsedJson);
        var emittedCounts = emitCountChangeAnswer.getResult();
        assertThat(emittedCounts.getEnemyUnread()).isEqualTo(enemyUnread);
        assertThat(emittedCounts.getUserUnread()).isEqualTo(userUnread);
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(MissionReportBo.MISSION_REPORT_CACHE_TAG)
                .targetBo(missionReportBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
