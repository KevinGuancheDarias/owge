package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.dto.base.DtoWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Getter
@Setter
public class SpeedImpactGroupDto extends DtoWithMissionLimitation implements DtoFromEntity<SpeedImpactGroup> {
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
        id = entity.getId();
        if (entity.getRequirementGroups() != null) {
            requirementsGroups = entity.getRequirementGroups().stream().map(current -> {
                var dto = new RequirementGroupDto();
                dto.dtoFromEntity(current);
                return dto;
            }).collect(Collectors.toList());
        }
        if (entity.getImage() != null) {
            image = entity.getImage().getId();
            imageUrl = entity.getImage().getUrl();
        }
    }

    private void initBaseData(SpeedImpactGroup entity) {
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
}
