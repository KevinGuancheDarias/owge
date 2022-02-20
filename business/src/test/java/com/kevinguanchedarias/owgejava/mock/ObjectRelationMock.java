package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ObjectRelationMock {
    public static final int REFERENCE_ID = 1918;

    public static ObjectRelation givenObjectRelation() {
        return ObjectRelation.builder()
                .object(givenObjectEntity())
                .referenceId(REFERENCE_ID)
                .build();
    }

    public static ObjectEntity givenObjectEntity() {
        var code = ObjectEnum.UPGRADE.name();
        return ObjectEntity.builder()
                .code(code)
                .description(code)
                .build();
    }
}
