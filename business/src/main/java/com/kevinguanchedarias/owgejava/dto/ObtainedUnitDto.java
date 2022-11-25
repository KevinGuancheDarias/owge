package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.jdbc.ObtainedUnitTemporalInformation;
import com.kevinguanchedarias.owgejava.entity.projection.ObtainedUnitBasicInfoProjection;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ObtainedUnitDto implements DtoFromEntity<ObtainedUnit> {

    @EqualsAndHashCode.Include
    private Long id;

    private UnitDto unit;
    private Long count;
    private PlanetDto sourcePlanet;
    private PlanetDto targetPlanet;
    private MissionDto mission;
    private Integer userId;
    private String username;
    private ObtainedUnitTemporalInformation temporalInformation;
    private List<ObtainedUnitBasicInfoProjection> storedUnits;

    @Override
    public void dtoFromEntity(ObtainedUnit entity) {
        id = entity.getId();
        unit = new UnitDto();
        unit.dtoFromEntity(entity.getUnit());
        count = entity.getCount();
        sourcePlanet = DtoUtilService.staticDtoFromEntity(PlanetDto.class, entity.getSourcePlanet());
        targetPlanet = DtoUtilService.staticDtoFromEntity(PlanetDto.class, entity.getTargetPlanet());
        mission = DtoUtilService.staticDtoFromEntity(MissionDto.class, entity.getMission());
        userId = entity.getUser().getId();
        username = entity.getUser().getUsername();
    }

}
