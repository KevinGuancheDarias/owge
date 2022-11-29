package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.Data;

import java.time.ZoneOffset;
import java.util.Date;

@Data
public abstract class AbstractRunningMissionDto {
    private static final int INTENTIONAL_DELAY_MS = 2000;
    private static final int NEVER_ENDING_MISSION_SYMBOL = -1;

    private Long missionId;
    private Double requiredPrimary;
    private Double requiredSecondary;
    private Double requiredTime;
    private Long pendingMillis;
    private MissionType type;
    private Integer missionsCount;

    protected AbstractRunningMissionDto() {
        throw new UnsupportedOperationException("Can't create a RunningMissionDto from an empty constructor");
    }

    protected DtoUtilService findDtoService() {
        return new DtoUtilService();
    }

    protected AbstractRunningMissionDto(Mission mission) {
        missionId = mission.getId();
        requiredPrimary = mission.getPrimaryResource();
        requiredSecondary = mission.getSecondaryResource();
        requiredTime = mission.getRequiredTime();
        pendingMillis = mission.getTerminationDate() == null ? NEVER_ENDING_MISSION_SYMBOL
                : (mission.getTerminationDate().toInstant(ZoneOffset.UTC).toEpochMilli() - new Date().getTime() + INTENTIONAL_DELAY_MS);
        type = MissionType.valueOf(mission.getType().getCode());
    }
}
