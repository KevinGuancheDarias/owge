package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenImprovement;

@UtilityClass
public class ImprovementUnitTypeMock {
    public static final long IMPROVEMENT_UNIT_TYPE_VALUE = 24;

    public static ImprovementUnitType givenImprovementUnitType(ImprovementTypeEnum improvementTypeEnum) {
        return ImprovementUnitType.builder()
                .improvementId(givenImprovement())
                .type(improvementTypeEnum.name())
                .value(IMPROVEMENT_UNIT_TYPE_VALUE)
                .build();
    }
}
