package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import lombok.Data;

/**
 * @author Kevin Guanche Darias
 * @since 0.9.0
 */
@Data
public class SpecialLocationDto extends CommonDtoWithImageStore<Integer, SpecialLocation>
        implements DtoWithImprovements {

    private ImprovementDto improvement;
    private Integer galaxyId;
    private String galaxyName;
    private Long assignedPlanetId;
    private String assignedPlanetName;

    @Override
    public void dtoFromEntity(SpecialLocation entity) {
        super.dtoFromEntity(entity);
        DtoWithImprovements.super.dtoFromEntity(entity);
        loadGalaxy(entity);
        Planet assignedPlanet = entity.getAssignedPlanet();
        if (assignedPlanet != null) {
            assignedPlanetId = assignedPlanet.getId();
            assignedPlanetName = assignedPlanet.getName();
        }
    }

    protected void loadGalaxy(SpecialLocation entity) {
        var galaxy = entity.getGalaxy();
        if (galaxy != null) {
            galaxyId = galaxy.getId();
            galaxyName = galaxy.getName();
        }
    }
}
