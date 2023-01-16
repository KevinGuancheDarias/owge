package com.kevinguanchedarias.owgejava.business.util;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitImprovementCalculationService;
import com.kevinguanchedarias.owgejava.entity.Unit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenImprovement;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = UnitImprovementUtilService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImprovementBo.class,
        ObtainedUnitImprovementCalculationService.class
})
class UnitImprovementUtilServiceTest {
    private final UnitImprovementUtilService unitImprovementUtilService;
    private final ImprovementBo improvementBo;
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;


    @Autowired
    UnitImprovementUtilServiceTest(
            UnitImprovementUtilService unitImprovementUtilService,
            ImprovementBo improvementBo,
            ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService
    ) {
        this.unitImprovementUtilService = unitImprovementUtilService;
        this.improvementBo = improvementBo;
        this.obtainedUnitImprovementCalculationService = obtainedUnitImprovementCalculationService;
    }

    @ParameterizedTest
    @MethodSource("maybeTriggerClearImprovement_should_work_arguments")
    void maybeTriggerClearImprovement_should_work(Unit unit, int timesClearSourceCache) {
        var ou = givenObtainedUnit1().toBuilder().unit(unit).build();
        var user = givenUser1();

        unitImprovementUtilService.maybeTriggerClearImprovement(user, List.of(ou));

        verify(improvementBo, times(timesClearSourceCache)).clearSourceCache(user, obtainedUnitImprovementCalculationService);
    }

    private static Stream<Arguments> maybeTriggerClearImprovement_should_work_arguments() {
        var unitWithImprovement = givenUnit1().toBuilder().improvement(givenImprovement()).build();
        return Stream.of(
                Arguments.of(givenUnit1(), 0),
                Arguments.of(unitWithImprovement, 1)
        );
    }
}
