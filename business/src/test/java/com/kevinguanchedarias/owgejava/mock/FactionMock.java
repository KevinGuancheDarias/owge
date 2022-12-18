package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.dto.FactionSpawnLocationDto;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.FactionSpawnLocation;
import com.kevinguanchedarias.owgejava.entity.FactionUnitType;
import com.kevinguanchedarias.owgejava.pojo.UnitTypesOverride;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_ID;

@UtilityClass
public class FactionMock {
    public static final int FACTION_ID = 1;
    public static final long SECTOR_RANGE_START = 12L;
    public static final long SECTOR_RANGE_END = 18L;
    public static final long QUADRANT_RANGE_START = 10L;
    public static final long QUADRANT_RANGE_END = 12L;
    public static final int FACTION_UNIT_TYPE_ID = 19;
    public static final int UNIT_TYPE_ID = 28;
    public static final long UNIT_TYPE_MAX_COUNT = 180;
    public static final long OVERRIDE_MAX_COUNT = 4L;
    public static final int FACTION_SPAWN_LOCATION_ID = 19;
    public static final int FACTION_INITIAL_PR = 2291;
    public static final int FACTION_INITIAL_SR = 2491;
    public static final int FACTION_INITIAL_ENERGY = 100;

    public static Faction givenFaction() {
        return Faction.builder()
                .id(FACTION_ID)
                .initialPrimaryResource(FACTION_INITIAL_PR)
                .initialSecondaryResource(FACTION_INITIAL_SR)
                .initialEnergy(FACTION_INITIAL_ENERGY)
                .improvement(ImprovementMock.givenImprovement())
                .build();
    }

    public static UnitTypesOverride givenOverride() {
        var retVal = new UnitTypesOverride();
        retVal.setOverrideMaxCount(OVERRIDE_MAX_COUNT);
        retVal.setId(UNIT_TYPE_ID);
        return retVal;
    }

    public static FactionUnitType givenFactionUnitType() {
        return FactionUnitType.builder()
                .id(FACTION_UNIT_TYPE_ID)
                .faction(givenFaction())
                .unitType(UnitTypeMock.givenUnitType(UNIT_TYPE_ID))
                .maxCount(UNIT_TYPE_MAX_COUNT)
                .build();
    }

    public static FactionSpawnLocation givenSpawnLocation() {
        return FactionSpawnLocation.builder()
                .id(FACTION_SPAWN_LOCATION_ID)
                .galaxy(GalaxyMock.givenGalaxy())
                .sectorRangeStart(SECTOR_RANGE_START)
                .sectorRangeEnd(SECTOR_RANGE_END)
                .quadrantRangeStart(QUADRANT_RANGE_START)
                .quadrantRangeEnd(QUADRANT_RANGE_END)
                .build();
    }

    public static FactionSpawnLocationDto givenSpawnLocationDto() {
        return FactionSpawnLocationDto.builder()
                .galaxyId(GALAXY_ID)
                .sectorRangeStart(SECTOR_RANGE_START)
                .sectorRangeEnd(SECTOR_RANGE_END)
                .quadrantRangeStart(QUADRANT_RANGE_START)
                .quadrantRangeEnd(QUADRANT_RANGE_END)
                .build();
    }
}
