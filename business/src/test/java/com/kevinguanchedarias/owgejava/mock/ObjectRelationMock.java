package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ObjectRelationMock {
    public static final int OBJECT_RELATION_ID = 41823;
    public static final int REFERENCE_ID = 1918;
    public static final long UNLOCKED_RELATION_ID = 2938;
    public static final ObjectEnum DTO_OBJECT_CODE = ObjectEnum.UPGRADE;
    public static final String DTO_OBJECT_CODE_NAME = DTO_OBJECT_CODE.name();

    public static ObjectRelation givenObjectRelation() {
        return givenObjectRelation(OBJECT_RELATION_ID);
    }

    public static ObjectRelation givenObjectRelation(Integer id) {
        return ObjectRelation.builder()
                .id(id)
                .object(givenObjectEntity())
                .referenceId(REFERENCE_ID)
                .build();
    }

    public static ObjectRelationDto givenObjectRelationDto() {
        return ObjectRelationDto.builder()
                .id(OBJECT_RELATION_ID)
                .objectCode(DTO_OBJECT_CODE_NAME)
                .referenceId(REFERENCE_ID)
                .build();
    }

    public static ObjectEntity givenObjectEntity() {
        return givenObjectEntity(ObjectEnum.UPGRADE);
    }

    public static ObjectEntity givenObjectEntity(ObjectEnum target) {
        var code = target.name();
        return ObjectEntity.builder()
                .code(code)
                .build();
    }

    public static UnlockedRelation givenUnlockedRelation(UserStorage user) {
        return UnlockedRelation.builder()
                .id(UNLOCKED_RELATION_ID)
                .relation(givenObjectRelation())
                .user(user)
                .build();
    }

    public static ObjectRelationToObjectRelation givenObjectRelationToObjectRelation() {
        return ObjectRelationToObjectRelation.builder()
                .slave(givenObjectRelation())
                .build();
    }
}
