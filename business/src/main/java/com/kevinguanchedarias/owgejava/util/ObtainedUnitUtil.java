package com.kevinguanchedarias.owgejava.util;

import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;

import java.util.List;

public class ObtainedUnitUtil {
    public static void handleInvisible(List<ObtainedUnitDto> obtainedUnitDtoList) {
        obtainedUnitDtoList.stream()
                .filter(involved -> involved.getUnit().getIsInvisible())
                .forEach(involved -> {
                    involved.setUnit(null);
                    involved.setCount(null);
                });
    }

    private ObtainedUnitUtil() {
        // An util class doesn't have constructor
    }
}
