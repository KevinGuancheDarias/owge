package com.kevinguanchedarias.owgejava.business.unit.obtained;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = ObtainedUnitModificationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUnitRepository.class,
        ImprovementBo.class,
        ObtainedUnitImprovementCalculationService.class
})
class ObtainedUnitModificationBoTest {
    private final ObtainedUnitModificationBo obtainedUnitModificationBo;
    private final ObtainedUnitRepository repository;
    private final ImprovementBo improvementBo;
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;

    @Autowired
    ObtainedUnitModificationBoTest(
            ObtainedUnitModificationBo obtainedUnitModificationBo,
            ObtainedUnitRepository repository,
            ImprovementBo improvementBo,
            ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService
    ) {
        this.obtainedUnitModificationBo = obtainedUnitModificationBo;
        this.repository = repository;
        this.improvementBo = improvementBo;
        this.obtainedUnitImprovementCalculationService = obtainedUnitImprovementCalculationService;
    }

    @Test
    void deleteByMissionId_should_work() {
        obtainedUnitModificationBo.deleteByMissionId(EXPLORE_MISSION_ID);

        verify(repository, times(1)).deleteByMissionId(EXPLORE_MISSION_ID);
        verify(improvementBo, times(1)).clearCacheEntries(obtainedUnitImprovementCalculationService);
    }
}
