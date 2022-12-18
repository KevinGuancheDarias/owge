package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Planet;
import lombok.Data;

@Data
public class PlanetDto implements DtoFromEntity<Planet> {
    private Long id;
    private String name;
    private Long sector;
    private Long quadrant;
    private Integer planetNumber;
    private Integer ownerId;
    private String ownerName;
    private Integer richness;
    private Boolean home;
    private Integer galaxyId;
    private String galaxyName;
    private SpecialLocationDto specialLocation;

    @Override
    public void dtoFromEntity(Planet entity) {
        copyBasicProperties(entity);
        if (entity.getOwner() != null) {
            ownerId = entity.getOwner().getId();
            ownerName = entity.getOwner().getUsername();
        }
        if (entity.getGalaxy() != null) {
            galaxyId = entity.getGalaxy().getId();
            galaxyName = entity.getGalaxy().getName();
        }
        if (entity.getSpecialLocation() != null) {
            specialLocation = new SpecialLocationDto();
            specialLocation.dtoFromEntity(entity.getSpecialLocation());
        }
    }

    private void copyBasicProperties(Planet entity) {
        id = entity.getId();
        name = entity.getName();
        sector = entity.getSector();
        quadrant = entity.getQuadrant();
        planetNumber = entity.getPlanetNumber();
        richness = entity.getRichness();
        home = entity.getHome();
    }
}
