package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the required information to register an "unit based mission"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Data
@NoArgsConstructor
@Builder(toBuilder = true)
@AllArgsConstructor
public class UnitMissionInformation {
    private Integer userId;
    private Long sourcePlanetId;
    private Long targetPlanetId;
    private MissionType missionType;
    private Long wantedTime;
    private List<SelectedUnit> involvedUnits;
}
