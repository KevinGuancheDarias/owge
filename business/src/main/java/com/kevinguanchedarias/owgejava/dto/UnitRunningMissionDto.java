package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;

import java.util.List;

/**
 * This pojo represents a mission where units are involved, such as explore,
 * gather. and such
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class UnitRunningMissionDto extends AbstractRunningMissionDto {
    private List<ObtainedUnitDto> involvedUnits;
    private Boolean invisible = false;
    private PlanetDto sourcePlanet;
    private PlanetDto targetPlanet;
    private UserStorageDto user;

    public UnitRunningMissionDto(Mission mission) {
        this(mission, mission.getInvolvedUnits());
    }

    public UnitRunningMissionDto(Mission mission, List<ObtainedUnit> involvedUnits) {
        super(mission);
        if (involvedUnits != null) {
            this.involvedUnits = findDtoService().convertEntireArray(ObtainedUnitDto.class, involvedUnits);
        }
        sourcePlanet = findDtoService().dtoFromEntity(PlanetDto.class, mission.getSourcePlanet());
        targetPlanet = findDtoService().dtoFromEntity(PlanetDto.class, mission.getTargetPlanet());
        var userEntity = mission.getUser();
        user = new UserStorageDto();
        if (userEntity.getAlliance() != null) {
            AllianceDto alliance = new AllianceDto();
            alliance.setId(userEntity.getAlliance().getId());
            user.setAlliance(alliance);
        }
        user.setId(userEntity.getId());
        user.setUsername(userEntity.getUsername());
    }

    /**
     * Defines as undefined the source and the target planet, of each involved unit
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public UnitRunningMissionDto nullifyInvolvedUnitsPlanets() {
        if (involvedUnits != null) {
            involvedUnits.forEach(current -> {
                current.setSourcePlanet(null);
                current.setTargetPlanet(null);
            });
        }
        return this;
    }

    public List<ObtainedUnitDto> getInvolvedUnits() {
        return involvedUnits;
    }

    public void setInvolvedUnits(List<ObtainedUnitDto> involvedUnits) {
        this.involvedUnits = involvedUnits;
    }

    public Boolean getInvisible() {
        return invisible;
    }

    public void setInvisible(Boolean invisible) {
        this.invisible = invisible;
    }

    public PlanetDto getSourcePlanet() {
        return sourcePlanet;
    }

    public void setSourcePlanet(PlanetDto sourcePlanet) {
        this.sourcePlanet = sourcePlanet;
    }

    public PlanetDto getTargetPlanet() {
        return targetPlanet;
    }

    public void setTargetPlanet(PlanetDto targetPlanet) {
        this.targetPlanet = targetPlanet;
    }

    public UserStorageDto getUser() {
        return user;
    }

    public void setUser(UserStorageDto user) {
        this.user = user;
    }

}
