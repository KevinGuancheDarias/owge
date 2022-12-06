package com.kevinguanchedarias.owgejava.pojo.websocket;

import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Represents the user missions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Value
@Builder
@AllArgsConstructor
public class MissionWebsocketMessage {
    int count;
    List<UnitRunningMissionDto> myUnitMissions;
}
