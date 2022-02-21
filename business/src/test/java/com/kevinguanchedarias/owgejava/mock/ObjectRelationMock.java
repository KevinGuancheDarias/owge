package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ObjectRelationMock {
    public static final int OBJECT_RELATION_ID = 41823;
    public static final int REFERENCE_ID = 1918;
    public static final long UNLOCKED_RELATION_ID = 2938;

    public static ObjectRelation givenObjectRelation() {
        return ObjectRelation.builder()
                .id(OBJECT_RELATION_ID)
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

    public static UnlockedRelation givenUnlockedRelation(UserStorage user) {
        return UnlockedRelation.builder()
                .id(UNLOCKED_RELATION_ID)
                .relation(givenObjectRelation())
                .user(user)
                .build();
    }
}
