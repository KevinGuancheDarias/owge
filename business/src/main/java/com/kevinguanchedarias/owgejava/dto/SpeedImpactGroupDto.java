package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.dto.base.DtoWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SpeedImpactGroupDto extends DtoWithMissionLimitation<SpeedImpactGroup> implements DtoFromEntity<SpeedImpactGroup> {
    @EqualsAndHashCode.Include
    private Integer id;

    private String name;
    private Boolean isFixed = false;
    private Double missionExplore = 0D;
    private Double missionGather = 0D;
    private Double missionEstablishBase = 0D;
    private Double missionAttack = 0D;
    private Double missionConquest = 0D;
    private Double missionCounterattack = 0D;
    private Long image;
    private String imageUrl;
    private List<RequirementGroupDto> requirementsGroups;

    @Override
    public void dtoFromEntity(SpeedImpactGroup entity) {
        initBaseData(entity);
        defineMissionLimitation(entity);
        loadImage(entity);
        if (entity.getRequirementGroups() != null) {
            requirementsGroups = entity.getRequirementGroups().stream().map(current -> {
                var dto = new RequirementGroupDto();
                dto.dtoFromEntity(current);
                return dto;
            }).toList();
        }
    }

    protected void initBaseData(SpeedImpactGroup entity) {
        id = entity.getId();
        name = entity.getName();
        isFixed = entity.getIsFixed();
        missionExplore = entity.getMissionExplore();
        missionGather = entity.getMissionGather();
        missionEstablishBase = entity.getMissionEstablishBase();
        missionAttack = entity.getMissionAttack();
        missionConquest = entity.getMissionConquest();
        missionCounterattack = entity.getMissionCounterattack();
    }

    protected void loadImage(SpeedImpactGroup entity) {
        if (entity.getImage() != null) {
            image = entity.getImage().getId();
            imageUrl = entity.getImage().getUrl();
        }
    }
}
