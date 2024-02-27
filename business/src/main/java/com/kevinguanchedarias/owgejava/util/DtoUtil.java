package com.kevinguanchedarias.owgejava.util;

import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoUtil {
    public static <K> K returnIdOrNull(EntityWithId<K> entity) {
        return entity != null
                ? entity.getId()
                : null;
    }
}
