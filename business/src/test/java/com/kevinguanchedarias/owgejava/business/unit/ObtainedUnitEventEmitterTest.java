package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.responses.UnitTypeResponse;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter.UNIT_OBTAINED_CHANGE;
import static com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter.UNIT_TYPE_CHANGE;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = ObtainedUnitEventEmitter.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        TransactionUtilService.class,
        SocketIoService.class,
        ObtainedUnitFinderBo.class,
        AsyncRunnerBo.class,
        UnitTypeBo.class,
        UserEventEmitterBo.class
})
class ObtainedUnitEventEmitterTest {
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final TransactionUtilService transactionUtilService;
    private final SocketIoService socketIoService;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;
    private final AsyncRunnerBo asyncRunnerBo;
    private final UnitTypeBo unitTypeBo;
    private final UserEventEmitterBo userEventEmitterBo;

    @Autowired
    ObtainedUnitEventEmitterTest(
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            TransactionUtilService transactionUtilService,
            SocketIoService socketIoService,
            ObtainedUnitFinderBo obtainedUnitFinderBo,
            AsyncRunnerBo asyncRunnerBo,
            UnitTypeBo unitTypeBo,
            UserEventEmitterBo userEventEmitterBo
    ) {
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.transactionUtilService = transactionUtilService;
        this.socketIoService = socketIoService;
        this.obtainedUnitFinderBo = obtainedUnitFinderBo;
        this.asyncRunnerBo = asyncRunnerBo;
        this.unitTypeBo = unitTypeBo;
        this.userEventEmitterBo = userEventEmitterBo;
    }

    @Test
    void emitObtainedUnitsAfterCommit_should_work() {
        var user = givenUser1();
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        var socketMessageAnswer = new InvokeSupplierLambdaAnswer<List<ObtainedUnitDto>>(2);
        doAnswer(socketMessageAnswer).when(socketIoService).sendMessage(eq(user), eq(UNIT_OBTAINED_CHANGE), any());
        var expectedSocketMessage = List.of(mock(ObtainedUnitDto.class));
        given(obtainedUnitFinderBo.findCompletedAsDto(user)).willReturn(expectedSocketMessage);

        obtainedUnitEventEmitter.emitObtainedUnitsAfterCommit(user);

        assertThat(socketMessageAnswer.getResult()).isSameAs(expectedSocketMessage);
    }

    @ParameterizedTest
    @MethodSource("emitSideChanges_should_work_arguments")
    void emitSideChanges_should_work(Unit unit, int timesEmitUserData) {
        var ou = givenObtainedUnit1();
        ou.setUnit(unit);
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(asyncRunnerBo).runAsyncWithoutContextDelayed(any());
        var socketMessageAnswer = new InvokeSupplierLambdaAnswer<List<UnitTypeResponse>>(2);
        doAnswer(socketMessageAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(UNIT_TYPE_CHANGE), any());
        var user = givenUser1();
        doAnswer(new InvokeSupplierLambdaAnswer<List<ObtainedUnitDto>>(2)).when(socketIoService).sendMessage(eq(user), eq(UNIT_OBTAINED_CHANGE), any());
        var expectedSocketMessage = List.of(mock(UnitTypeResponse.class));
        given(unitTypeBo.findUnitTypesWithUserInfo(USER_ID_1)).willReturn(expectedSocketMessage);

        obtainedUnitEventEmitter.emitSideChanges(List.of(ou));

        verify(userEventEmitterBo, times(timesEmitUserData)).emitUserData(user);
        verify(obtainedUnitFinderBo, times(1)).findCompletedAsDto(user);
        assertThat(socketMessageAnswer.getResult()).isSameAs(expectedSocketMessage);
    }

    @Test
    void emitSideChanges_should_do_nothing_on_empty_list() {
        obtainedUnitEventEmitter.emitSideChanges(List.of());
        verifyNoInteractions(asyncRunnerBo, userEventEmitterBo, unitTypeBo, obtainedUnitFinderBo);
    }

    private static Stream<Arguments> emitSideChanges_should_work_arguments() {
        var unitWithEnergy = givenUnit1().toBuilder().energy(20).build();
        var unitWithoutEnergy = givenUnit1();
        var unitWithZeroEnergy = givenUnit1().toBuilder().energy(0).build();
        return Stream.of(
                Arguments.of(unitWithEnergy, 1),
                Arguments.of(unitWithoutEnergy, 0),
                Arguments.of(unitWithZeroEnergy, 0)
        );
    }
}
