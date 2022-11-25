package com.kevinguanchedarias.owgejava.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Represents the units that should be send to the mission
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Value
@Builder
@AllArgsConstructor
@Jacksonized
public class SelectedUnit {
    Integer id;
    Long count;
    Long expirationId;
    List<SelectedUnit> storedUnits;
}
