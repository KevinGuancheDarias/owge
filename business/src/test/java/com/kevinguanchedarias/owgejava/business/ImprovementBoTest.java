package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ImprovementUnitTypeDto;
import com.kevinguanchedarias.owgejava.mock.ImprovementUnitTypeMock;
import com.kevinguanchedarias.owgejava.repository.ImprovementRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ImprovementBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImprovementRepository.class,
        DtoUtilService.class,
        CacheManager.class,
        ConfigurationBo.class,
        SocketIoService.class
})
class ImprovementBoTest {
    private final ImprovementBo improvementBo;
    private final ImprovementRepository repository;
    private final DtoUtilService dtoUtilService;
    private final transient CacheManager cacheManager;
    private final ConfigurationBo configurationBo;
    private final transient SocketIoService socketIoService;
    private final transient BeanFactory beanFactory;

    @Autowired
    ImprovementBoTest(
            ImprovementBo improvementBo,
            ImprovementRepository repository,
            DtoUtilService dtoUtilService,
            CacheManager cacheManager,
            ConfigurationBo configurationBo,
            SocketIoService socketIoService,
            BeanFactory beanFactory
    ) {
        this.improvementBo = improvementBo;
        this.repository = repository;
        this.dtoUtilService = dtoUtilService;
        this.cacheManager = cacheManager;
        this.configurationBo = configurationBo;
        this.socketIoService = socketIoService;
        this.beanFactory = beanFactory;
    }

    @Test
    void multiplyValues_should_work() {
        var improvement = givenImprovement();
        int count = 4;

        var result = improvementBo.multiplyValues(improvement, count);

        assertThat(result.getMoreChargeCapacity()).isEqualTo(MORE_CHARGE_CAPACITY * count);
        assertThat(result.getMoreEnergyProduction()).isEqualTo(MORE_ENERGY * count);
        assertThat(result.getMoreMissions()).isEqualTo(MORE_MISSIONS * count);
        assertThat(result.getMorePrimaryResourceProduction()).isEqualTo(MORE_PR * count);
        assertThat(result.getMoreSecondaryResourceProduction()).isEqualTo(MORE_SR * count);
        assertThat(result.getMoreUnitBuildSpeed()).isEqualTo(MORE_UNIT_BUILD_SPEED * count);
        assertThat(result.getMoreUpgradeResearchSpeed()).isEqualTo(MORE_UPGRADE_RESEARCH_SPEED * count);
        assertThat(result.getUnitTypesUpgrades())
                .hasSize(1)
                .extracting(ImprovementUnitTypeDto::getValue)
                .containsExactly(ImprovementUnitTypeMock.IMPROVEMENT_UNIT_TYPE_VALUE * count);
    }
}
