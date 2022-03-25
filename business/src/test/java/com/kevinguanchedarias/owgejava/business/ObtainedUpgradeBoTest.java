package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ImprovementDto;
import com.kevinguanchedarias.owgejava.dto.ObtainedUpgradeDto;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenEntity;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUpgradeMock.OBTAINED_UPGRADE_ID;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUpgradeMock.OBTAINED_UPGRADE_LEVEL;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUpgradeMock.givenObtainedUpgrade;
import static com.kevinguanchedarias.owgejava.mock.UpgradeTypeMock.givenUpgradeType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = ObtainedUpgradeBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUpgradeRepository.class,
        ImprovementBo.class,
        SocketIoService.class,
        TaggableCacheManager.class
})
class ObtainedUpgradeBoTest extends AbstractBaseBoTest {
    private final ObtainedUpgradeBo obtainedUpgradeBo;
    private final TaggableCacheManager taggableCacheManager;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;
    private final ImprovementBo improvementBo;
    private final SocketIoService socketIoService;

    @Autowired
    public ObtainedUpgradeBoTest(
            ObtainedUpgradeBo obtainedUpgradeBo,
            TaggableCacheManager taggableCacheManager,
            ObtainedUpgradeRepository obtainedUpgradeRepository,
            ImprovementBo improvementBo,
            SocketIoService socketIoService
    ) {
        this.obtainedUpgradeBo = obtainedUpgradeBo;
        this.taggableCacheManager = taggableCacheManager;
        this.obtainedUpgradeRepository = obtainedUpgradeRepository;
        this.improvementBo = improvementBo;
        this.socketIoService = socketIoService;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ObtainedUpgradeBo.OBTAINED_UPGRADE_CACHE_TAG)
                .targetBo(obtainedUpgradeBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }

    @Test
    void calculateImprovements_should_work() {
        var ou = givenObtainedUpgrade();
        var improvement = givenEntity();
        ou.getUpgrade().setImprovement(improvement);
        var multipliedResult = new ImprovementDto();
        multipliedResult.setId(2);
        multipliedResult.setMoreMisions(2F);
        given(obtainedUpgradeRepository.findByUserId(USER_ID_1)).willReturn(List.of(ou));
        given(improvementBo.multiplyValues(improvement, ou.getLevel())).willReturn(multipliedResult);


        var result = obtainedUpgradeBo.calculateImprovement(ou.getUser());

        assertThat(result.getMoreMisions()).isEqualTo(2F);

    }

    @Test
    void emitObtainedChange_should_work() {
        var ou = givenObtainedUpgrade();
        ou.getUpgrade().setType(givenUpgradeType());
        var supplierAnswer = new InvokeSupplierLambdaAnswer<List<ObtainedUpgradeDto>>(2);
        doAnswer(supplierAnswer).when(socketIoService)
                .sendMessage(eq(USER_ID_1), eq(ObtainedUpgradeBo.OBTANED_UPGRADE_CHANGE_EVENT), any());
        given(obtainedUpgradeRepository.findByUserId(USER_ID_1)).willReturn(List.of(ou));

        obtainedUpgradeBo.emitObtainedChange(USER_ID_1);

        verify(socketIoService, times(1)).sendMessage(eq(USER_ID_1), eq(ObtainedUpgradeBo.OBTANED_UPGRADE_CHANGE_EVENT), any());
        var sentData = supplierAnswer.getResult();
        assertThat(sentData).hasSize(1);
        var ouDto = sentData.get(0);
        assertThat(ouDto.getId()).isEqualTo(OBTAINED_UPGRADE_ID);
        assertThat(ouDto.getLevel()).isEqualTo(OBTAINED_UPGRADE_LEVEL);
        assertThat(ouDto.getUpgrade()).isNotNull();

    }

}
