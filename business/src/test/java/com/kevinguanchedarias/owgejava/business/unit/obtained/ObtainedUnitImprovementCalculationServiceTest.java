package com.kevinguanchedarias.owgejava.business.unit.obtained;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenImprovement;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = ObtainedUnitImprovementCalculationService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUnitRepository.class,
        ImprovementBo.class
})
class ObtainedUnitImprovementCalculationServiceTest {
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;
    private final ObtainedUnitRepository repository;
    private final ImprovementBo improvementBo;

    @Autowired
    ObtainedUnitImprovementCalculationServiceTest(
            ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService,
            ObtainedUnitRepository repository,
            ImprovementBo improvementBo
    ) {
        this.obtainedUnitImprovementCalculationService = obtainedUnitImprovementCalculationService;
        this.repository = repository;
        this.improvementBo = improvementBo;
    }

    @Test
    void init_should_work() {
        verify(improvementBo, times(1)).addImprovementSource(obtainedUnitImprovementCalculationService);
    }

    @Test
    void calculateImprovement_should_work() {
        var ou = givenObtainedUnit1();
        var improvement = givenImprovement();
        ou.getUnit().setImprovement(improvement);
        given(repository.findByUserAndNotBuilding(USER_ID_1)).willReturn(List.of(ou));

        try (var mockedConstruction = mockConstruction(GroupedImprovement.class)) {
            var retVal = obtainedUnitImprovementCalculationService.calculateImprovement(givenUser1());

            var groupedImprovementMock = mockedConstruction.constructed().get(0);
            verify(groupedImprovementMock, times(1)).add(improvement);
            assertThat(retVal).isSameAs(groupedImprovementMock);
        }
    }
}
