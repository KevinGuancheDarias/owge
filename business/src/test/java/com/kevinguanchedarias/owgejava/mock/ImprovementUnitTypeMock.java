package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ImprovementUnitTypeMock {
    public static final long IMPROVEMENT_UNIT_TYPE_VALUE = 24;

    public static ImprovementUnitType givenImprovementUnitType(ImprovementTypeEnum improvementTypeEnum) {
        return ImprovementUnitType.builder()
                .improvementId(Improvement.builder().id(1).build())
                .type(improvementTypeEnum.name())
                .value(IMPROVEMENT_UNIT_TYPE_VALUE)
                .build();
    }
}
