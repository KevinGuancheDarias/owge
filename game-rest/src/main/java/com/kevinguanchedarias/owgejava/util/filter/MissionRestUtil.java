package com.kevinguanchedarias.owgejava.util.filter;

import com.kevinguanchedarias.owgejava.dto.AbstractRunningMissionDto;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class MissionRestUtil {

    public static <E extends AbstractRunningMissionDto> List<E> mutateRecalculatePendingMillis(List<E> abstractRunningMissionDtoList) {
        abstractRunningMissionDtoList.forEach(AbstractRunningMissionDto::recalculatePendingMillis);
        return abstractRunningMissionDtoList;
    }
}
