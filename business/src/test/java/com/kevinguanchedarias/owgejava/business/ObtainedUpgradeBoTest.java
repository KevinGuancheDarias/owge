package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ImprovementDto;
import com.kevinguanchedarias.owgejava.dto.ObtainedUpgradeDto;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenImprovement;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUpgradeMock.*;
import static com.kevinguanchedarias.owgejava.mock.UpgradeTypeMock.givenUpgradeType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = ObtainedUpgradeBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUpgradeRepository.class,
        ImprovementBo.class,
        SocketIoService.class
})
class ObtainedUpgradeBoTest {
    private final ObtainedUpgradeBo obtainedUpgradeBo;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;
    private final ImprovementBo improvementBo;
    private final SocketIoService socketIoService;

    @Autowired
    public ObtainedUpgradeBoTest(
            ObtainedUpgradeBo obtainedUpgradeBo,
            ObtainedUpgradeRepository obtainedUpgradeRepository,
            ImprovementBo improvementBo,
            SocketIoService socketIoService
    ) {
        this.obtainedUpgradeBo = obtainedUpgradeBo;
        this.obtainedUpgradeRepository = obtainedUpgradeRepository;
        this.improvementBo = improvementBo;
        this.socketIoService = socketIoService;
    }

    @Test
    void calculateImprovements_should_work() {
        var ou = givenObtainedUpgrade();
        var improvement = givenImprovement();
        ou.getUpgrade().setImprovement(improvement);
        var multipliedResult = new ImprovementDto();
        multipliedResult.setId(2);
        multipliedResult.setMoreMissions(2F);
        given(obtainedUpgradeRepository.findByUserId(USER_ID_1)).willReturn(List.of(ou));
        given(improvementBo.multiplyValues(improvement, ou.getLevel())).willReturn(multipliedResult);


        var result = obtainedUpgradeBo.calculateImprovement(ou.getUser());

        assertThat(result.getMoreMissions()).isEqualTo(2F);

    }

    @Test
    void emitObtainedChange_should_work() {
        var ou = givenObtainedUpgrade();
        ou.getUpgrade().setType(givenUpgradeType());
        var supplierAnswer = new InvokeSupplierLambdaAnswer<List<ObtainedUpgradeDto>>(2);
        doAnswer(supplierAnswer).when(socketIoService)
                .sendMessage(eq(USER_ID_1), eq(ObtainedUpgradeBo.OBTAINED_UPGRADES_CHANGE), any());
        given(obtainedUpgradeRepository.findByUserId(USER_ID_1)).willReturn(List.of(ou));

        obtainedUpgradeBo.emitObtainedChange(USER_ID_1);

        verify(socketIoService, times(1)).sendMessage(eq(USER_ID_1), eq(ObtainedUpgradeBo.OBTAINED_UPGRADES_CHANGE), any());
        var sentData = supplierAnswer.getResult();
        assertThat(sentData).hasSize(1);
        var ouDto = sentData.get(0);
        assertThat(ouDto.getId()).isEqualTo(OBTAINED_UPGRADE_ID);
        assertThat(ouDto.getLevel()).isEqualTo(OBTAINED_UPGRADE_LEVEL);
        assertThat(ouDto.getUpgrade()).isNotNull();

    }

}
