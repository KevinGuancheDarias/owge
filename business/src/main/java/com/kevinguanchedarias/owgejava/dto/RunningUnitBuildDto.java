package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.Unit;

public class RunningUnitBuildDto extends AbstractRunningMissionDto {

    private final Long count;

    private UnitDto unit;
    private PlanetDto sourcePlanet;

    public RunningUnitBuildDto(Unit unit, Mission mission, Planet planet, Long count) {
        super(mission);
        this.unit = new UnitDto();
        this.unit.dtoFromEntity(unit);
        this.count = count;
        sourcePlanet = new PlanetDto();
        sourcePlanet.dtoFromEntity(planet);
    }

    public UnitDto getUnit() {
        return unit;
    }

    public void setUnit(UnitDto unit) {
        this.unit = unit;
    }

    public Long getCount() {
        return count;
    }

    /**
     * @return the sourcePlanet
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public PlanetDto getSourcePlanet() {
        return sourcePlanet;
    }

    /**
     * @param sourcePlanet the sourcePlanet to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setSourcePlanet(PlanetDto sourcePlanet) {
        this.sourcePlanet = sourcePlanet;
    }

}
