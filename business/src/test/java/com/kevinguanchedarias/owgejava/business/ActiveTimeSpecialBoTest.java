package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.timespecial.UnlockableTimeSpecialService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.dto.ActiveTimeSpecialDto;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructActiveTimeSpecialBo;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeConsumerLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.configuration.SpyEventPublisherConfiguration;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.*;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = NonPostConstructActiveTimeSpecialBo.class
)
@MockBean({
        ActiveTimeSpecialRepository.class,
        TimeSpecialBo.class,
        ObjectRelationBo.class,
        UserSessionService.class,
        ImprovementBo.class,
        ScheduledTasksManagerService.class,
        DtoUtilService.class,
        SocketIoService.class,
        RequirementBo.class,
        RuleRepository.class,
        ObtainedUnitEventEmitter.class,
        UnlockableTimeSpecialService.class
})
@Import(SpyEventPublisherConfiguration.class)
class ActiveTimeSpecialBoTest {
    private final NonPostConstructActiveTimeSpecialBo activeTimeSpecialBo;
    private final TimeSpecialBo timeSpecialBo;
    private final ObjectRelationBo objectRelationBo;
    private final UserSessionService userSessionService;
    private final ImprovementBo improvementBo;
    private final ScheduledTasksManagerService scheduledTasksManagerService;
    private final RequirementBo requirementBo;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final SocketIoService socketIoService;
    private final RuleRepository ruleRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final UnlockableTimeSpecialService unlockableTimeSpecialService;

    @Autowired
    ActiveTimeSpecialBoTest(
            NonPostConstructActiveTimeSpecialBo activeTimeSpecialBo,
            TimeSpecialBo timeSpecialBo,
            ObjectRelationBo objectRelationBo,
            UserSessionService userSessionService,
            ImprovementBo improvementBo,
            ScheduledTasksManagerService scheduledTasksManagerService,
            RequirementBo requirementBo,
            ActiveTimeSpecialRepository activeTimeSpecialRepository,
            SocketIoService socketIoService,
            RuleRepository ruleRepository,
            ApplicationEventPublisher applicationEventPublisher,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            UnlockableTimeSpecialService unlockableTimeSpecialService
    ) {
        this.activeTimeSpecialBo = activeTimeSpecialBo;
        this.timeSpecialBo = timeSpecialBo;
        this.objectRelationBo = objectRelationBo;
        this.improvementBo = improvementBo;
        this.userSessionService = userSessionService;
        this.scheduledTasksManagerService = scheduledTasksManagerService;
        this.requirementBo = requirementBo;
        this.activeTimeSpecialRepository = activeTimeSpecialRepository;
        this.socketIoService = socketIoService;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.ruleRepository = ruleRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.unlockableTimeSpecialService = unlockableTimeSpecialService;
    }

    @Test
    void getDtoClass_should_work() {
        assertThat(activeTimeSpecialBo.getDtoClass()).isEqualTo(ActiveTimeSpecialDto.class);
    }

    @Test
    void init_should_register_for_improvements_and_task_handlers() {

        this.activeTimeSpecialBo.realInit();

        verify(improvementBo, times(1)).addImprovementSource(activeTimeSpecialBo);
        verify(scheduledTasksManagerService, times(1)).addHandler(eq("TIME_SPECIAL_EFFECT_END"), any());
        verify(scheduledTasksManagerService, times(1)).addHandler(eq("TIME_SPECIAL_IS_READY"), any());
    }

    @Test
    void time_special_effect_end_handler_should_do_nothing_if_side_deleted(CapturedOutput capturedOutput) {
        var task = ScheduledTask.builder()
                .content(ACTIVE_TIME_SPECICAL_ID)
                .build();
        var lambdaAnswer = new InvokeConsumerLambdaAnswer<ScheduledTask>(1);
        doAnswer(lambdaAnswer)
                .when(scheduledTasksManagerService).addHandler(eq("TIME_SPECIAL_EFFECT_END"), any());

        activeTimeSpecialBo.realInit();
        lambdaAnswer.getPassedLambda().accept(task);

        verify(activeTimeSpecialRepository, times(1)).findById(ACTIVE_TIME_SPECICAL_ID);
        verify(improvementBo, never()).clearSourceCache(any(), eq(activeTimeSpecialBo));
        verify(scheduledTasksManagerService, never()).registerEvent(any(), anyLong());
        verify(requirementBo, never()).triggerTimeSpecialStateChange(any(), any());
        verify(socketIoService, never()).sendMessage(any(), any(), any());
        assertThat(capturedOutput.getOut())
                .contains("ActiveTimeSpecial was deleted outside");
    }

    @ParameterizedTest
    @CsvSource({
            "1,true",
            "0,false"
    })
    void time_special_effect_end_handler_should_put_time_special_in_recharge_state(int emitUnitsTimes, boolean isAffectingUnits, CapturedOutput capturedOutput) {
        var activeTimeSpecial = givenActiveTimeSpecial();
        var user = givenUser1();
        activeTimeSpecial.setUser(user);
        var task = ScheduledTask.builder()
                .content(ACTIVE_TIME_SPECICAL_ID)
                .build();
        var lambdaAnswer = new InvokeConsumerLambdaAnswer<ScheduledTask>(1);
        doAnswer(lambdaAnswer)
                .when(scheduledTasksManagerService).addHandler(eq("TIME_SPECIAL_EFFECT_END"), any());

        given(activeTimeSpecialRepository.findById(ACTIVE_TIME_SPECICAL_ID))
                .willReturn(Optional.of(activeTimeSpecial));
        given(ruleRepository.existsByOriginTypeAndOriginIdAndDestinationTypeIn(
                ObjectEnum.TIME_SPECIAL.name(), (long) TIME_SPECIAL_ID, List.of(ObjectEnum.UNIT.name(), "UNIT_TYPE"))
        ).willReturn(isAffectingUnits);

        activeTimeSpecialBo.realInit();

        verify(improvementBo, never()).clearSourceCache(any(), any());
        lambdaAnswer.getPassedLambda().accept(task);
        assertThat(capturedOutput.getOut()).contains("Time special effect end");
        verify(activeTimeSpecialRepository, times(1)).findById(ACTIVE_TIME_SPECICAL_ID);
        var captor = ArgumentCaptor.forClass(ActiveTimeSpecial.class);
        verify(activeTimeSpecialRepository, times(1)).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getState()).isEqualTo(TimeSpecialStateEnum.RECHARGE);
        assertThat(saved.getReadyDate()).isNotNull();
        verify(improvementBo, times(1)).clearSourceCache(user, activeTimeSpecialBo);
        var taskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(scheduledTasksManagerService, times(1)).registerEvent(taskCaptor.capture(), eq(ACTIVE_TIME_SPECIAL_RECHARGE_TIME));
        var registeredTask = taskCaptor.getValue();
        assertThat(registeredTask.getType()).isEqualTo("TIME_SPECIAL_IS_READY");
        assertThat(registeredTask.getContent()).isEqualTo(ACTIVE_TIME_SPECICAL_ID);
        verify(socketIoService, times(1)).sendMessage(eq(user), eq("time_special_change"), any());
        verify(requirementBo, times(1)).triggerTimeSpecialStateChange(user, activeTimeSpecial.getTimeSpecial());
        verify(obtainedUnitEventEmitter, times(emitUnitsTimes)).emitObtainedUnits(user);
    }

    @Test
    void time_special_is_ready_handler_should_put_time_special_as_available_to_activate(CapturedOutput capturedOutput) {
        var activeTimeSpecial = givenActiveTimeSpecial();
        var user = givenUser1();
        activeTimeSpecial.setUser(user);
        var task = ScheduledTask.builder()
                .content(Double.parseDouble(Long.toString(ACTIVE_TIME_SPECICAL_ID)))
                .build();
        var lambdaAnswer = new InvokeConsumerLambdaAnswer<ScheduledTask>(1);
        var socketMessageAnswer = new InvokeSupplierLambdaAnswer<List<TimeSpecialDto>>(2);
        var unlockedTimeSpecial = givenTimeSpecial();
        var unlockedTimeSpecialDto = new TimeSpecialDto();
        var unlockedList = List.of(unlockedTimeSpecial);

        doAnswer(lambdaAnswer)
                .when(scheduledTasksManagerService).addHandler(eq("TIME_SPECIAL_IS_READY"), any());
        doAnswer(socketMessageAnswer)
                .when(socketIoService).sendMessage(eq(user), eq("time_special_change"), any());
        given(activeTimeSpecialRepository.findById(ACTIVE_TIME_SPECICAL_ID))
                .willReturn(Optional.of(activeTimeSpecial));
        given(unlockableTimeSpecialService.findUnlocked(user)).willReturn(unlockedList);
        given(timeSpecialBo.toDto(unlockedList)).willReturn(List.of(unlockedTimeSpecialDto));

        activeTimeSpecialBo.realInit();
        lambdaAnswer.getPassedLambda().accept(task);

        verify(activeTimeSpecialRepository, times(1)).findById(ACTIVE_TIME_SPECICAL_ID);
        assertThat(capturedOutput.getOut()).contains("Time special becomes ready, deleting from ActiveTimeSpecial entry with id " + ACTIVE_TIME_SPECICAL_ID);
        verify(activeTimeSpecialRepository, times(1)).delete(activeTimeSpecial);
        verify(socketIoService, times(1)).sendMessage(eq(user), eq("time_special_change"), any());
        verify(unlockableTimeSpecialService, times(1)).findUnlocked(user);
        verify(timeSpecialBo, times(1)).toDto(unlockedList);
        var sentMessage = socketMessageAnswer.getResult();
        assertThat(sentMessage)
                .hasSize(1)
                .contains(unlockedTimeSpecialDto);

    }

    @Test
    void time_special_is_ready_handler_should_do_nothing_if_side_deleted_maybe_by_admin() {
        var lambdaAnswer = new InvokeConsumerLambdaAnswer<ScheduledTask>(1);
        var task = ScheduledTask.builder()
                .content(Double.parseDouble(Long.toString(ACTIVE_TIME_SPECICAL_ID)))
                .build();

        doAnswer(lambdaAnswer)
                .when(scheduledTasksManagerService).addHandler(eq("TIME_SPECIAL_IS_READY"), any());

        activeTimeSpecialBo.realInit();
        lambdaAnswer.getPassedLambda().accept(task);

        verify(activeTimeSpecialRepository, times(1)).findById(ACTIVE_TIME_SPECICAL_ID);
        verify(activeTimeSpecialRepository, never()).delete(any());
        verify(socketIoService, never()).sendMessage(any(UserStorage.class), any(), any());
    }

    @ParameterizedTest
    @CsvSource({
            "1,true",
            "0,false"
    })
    void activate_should_trigger_requirements(int emitUnitsTimes, boolean isAffectingUnits) {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        var or = givenObjectRelation();
        var activeTimeSpecialId = 19278123L;
        given(timeSpecialBo.findByIdOrDie(TIME_SPECIAL_ID)).willReturn(timeSpecial);
        given(objectRelationBo.findOne(ObjectEnum.TIME_SPECIAL, TIME_SPECIAL_ID))
                .willReturn(or);
        given(userSessionService.findLoggedIn()).willReturn(user);
        given(userSessionService.findLoggedInWithDetails()).willReturn(user);
        given(activeTimeSpecialRepository.save(any())).willAnswer(invocationOnMock -> {
            var newActivated = invocationOnMock.getArgument(0, ActiveTimeSpecial.class);
            newActivated.setId(activeTimeSpecialId);
            return newActivated;
        });
        given(ruleRepository.existsByOriginTypeAndOriginIdAndDestinationTypeIn(
                ObjectEnum.TIME_SPECIAL.name(), (long) TIME_SPECIAL_ID, List.of(ObjectEnum.UNIT.name(), "UNIT_TYPE"))
        ).willReturn(isAffectingUnits);
        doNothing().when(applicationEventPublisher).publishEvent(any(ActiveTimeSpecial.class));

        var result = activeTimeSpecialBo.activate(TIME_SPECIAL_ID);

        verify(timeSpecialBo, times(1)).findByIdOrDie(TIME_SPECIAL_ID);
        verify(objectRelationBo, times(1)).findOne(ObjectEnum.TIME_SPECIAL, TIME_SPECIAL_ID);
        verify(userSessionService, times(1)).findLoggedIn();
        verify(objectRelationBo, times(1)).checkIsUnlocked(user, or);
        verify(activeTimeSpecialRepository, times(1)).findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1);
        verify(userSessionService, times(1)).findLoggedInWithDetails();
        var captor = ArgumentCaptor.forClass(ActiveTimeSpecial.class);
        verify(activeTimeSpecialRepository, times(1)).save(captor.capture());
        verify(improvementBo, times(1)).clearSourceCache(user, activeTimeSpecialBo);
        var schedulerCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(scheduledTasksManagerService, times(1)).registerEvent(schedulerCaptor.capture(), eq(TIME_SPECIAL_DURATION));
        verify(socketIoService, times(1)).sendMessage(eq(user), eq("time_special_change"), any());
        var savedActive = captor.getValue();
        assertThat(savedActive).isSameAs(result);
        assertThat(savedActive.getTimeSpecial()).isEqualTo(timeSpecial);
        assertThat(savedActive.getState()).isEqualTo(TimeSpecialStateEnum.ACTIVE);
        var scheduledTask = schedulerCaptor.getValue();
        assertThat(scheduledTask.getType()).isEqualTo("TIME_SPECIAL_EFFECT_END");
        assertThat(scheduledTask.getContent()).isEqualTo(activeTimeSpecialId);
        verify(requirementBo, times(1)).triggerTimeSpecialStateChange(user, timeSpecial);
        verify(obtainedUnitEventEmitter, times(emitUnitsTimes)).emitObtainedUnits(user);
        verify(applicationEventPublisher, times(1)).publishEvent(savedActive);
    }

    @Test
    void activate_should_do_nothing_if_already_active(CapturedOutput capturedOutput) {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        var or = givenObjectRelation();
        var activeTimeSpecial = givenActiveTimeSpecial();
        given(timeSpecialBo.findByIdOrDie(TIME_SPECIAL_ID)).willReturn(timeSpecial);
        given(objectRelationBo.findOne(ObjectEnum.TIME_SPECIAL, TIME_SPECIAL_ID))
                .willReturn(or);
        given(userSessionService.findLoggedIn()).willReturn(user);
        given(activeTimeSpecialRepository.findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1))
                .willReturn(Optional.of(activeTimeSpecial));

        var result = activeTimeSpecialBo.activate(TIME_SPECIAL_ID);

        verify(timeSpecialBo, times(1)).findByIdOrDie(TIME_SPECIAL_ID);
        verify(objectRelationBo, times(1)).findOne(ObjectEnum.TIME_SPECIAL, TIME_SPECIAL_ID);
        verify(userSessionService, times(1)).findLoggedIn();
        verify(objectRelationBo, times(1)).checkIsUnlocked(user, or);
        verify(activeTimeSpecialRepository, times(1)).findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1);
        verify(userSessionService, never()).findLoggedInWithDetails();
        verify(improvementBo, never()).clearSourceCache(any(), any());
        verify(scheduledTasksManagerService, never()).registerEvent(any(), anyLong());
        verify(requirementBo, never()).triggerTimeSpecialStateChange(any(), any());
        verify(socketIoService, never()).sendMessage(any(UserStorage.class), any(), any());
        assertThat(capturedOutput.getOut()).contains("The specified time special, is already active, doing nothing");
        assertThat(result).isSameAs(activeTimeSpecial);
        verify(obtainedUnitEventEmitter, never()).emitObtainedUnits(any());
    }

    @Test
    void relationLost_should_do_nothing_if_not_a_time_special() {
        var ur = givenUnlockedRelation();

        activeTimeSpecialBo.relationLost(ur);

        verifyNoInteractions(activeTimeSpecialRepository, improvementBo, scheduledTasksManagerService);
    }

    @Test
    void relationLost_should_do_nothing_if_not_active() {
        var ur = givenUnlockedRelation();
        var or = ur.getRelation();
        or.setReferenceId(TIME_SPECIAL_ID);
        or.setObject(givenObjectEntity(ObjectEnum.TIME_SPECIAL));
        var ats = givenActiveTimeSpecial();
        ats.setState(TimeSpecialStateEnum.RECHARGE);
        given(activeTimeSpecialRepository.findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1)).willReturn(Optional.of(ats));

        activeTimeSpecialBo.relationLost(ur);

        verify(activeTimeSpecialRepository, times(1)).findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1);
        verifyNoInteractions(improvementBo, scheduledTasksManagerService);
    }

    @Test
    void relationLost_should_work() {
        var ur = givenUnlockedRelation();
        var user = ur.getUser();
        var or = ur.getRelation();
        or.setReferenceId(TIME_SPECIAL_ID);
        or.setObject(givenObjectEntity(ObjectEnum.TIME_SPECIAL));
        var ats = givenActiveTimeSpecial();
        var ts = ats.getTimeSpecial();
        ats.setUser(user);
        ats.setExpiringDate(new Date());
        ats.setState(TimeSpecialStateEnum.ACTIVE);
        given(activeTimeSpecialRepository.findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1)).willReturn(Optional.of(ats));
        given(activeTimeSpecialRepository.findById(ACTIVE_TIME_SPECICAL_ID)).willReturn(Optional.of(ats));

        activeTimeSpecialBo.relationLost(ur);

        var captor = ArgumentCaptor.forClass(ActiveTimeSpecial.class);
        verify(activeTimeSpecialRepository, times(1)).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getState()).isEqualTo(TimeSpecialStateEnum.RECHARGE);
        verify(requirementBo, times(1)).triggerTimeSpecialStateChange(user, ts);
    }

    @Test
    void order_should_return_zero() {
        assertThat(activeTimeSpecialBo.order()).isZero();
    }

    @Test
    void doDeleteUser_should_work() {
        var user = givenUser1();

        activeTimeSpecialBo.doDeleteUser(user);

        verify(activeTimeSpecialRepository, times(1)).deleteByUser(user);
    }
}
