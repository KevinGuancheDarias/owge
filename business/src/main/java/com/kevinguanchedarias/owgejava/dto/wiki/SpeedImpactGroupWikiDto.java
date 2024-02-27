package com.kevinguanchedarias.owgejava.dto.wiki;

import com.kevinguanchedarias.owgejava.dto.SpeedImpactGroupDto;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SpeedImpactGroupWikiDto extends SpeedImpactGroupDto {
    private List<Integer> requirementGroupIds;

    @Override
    public void dtoFromEntity(SpeedImpactGroup entity) {
        initBaseData(entity);
        defineMissionLimitation(entity);
        loadImage(entity);
        var requirementGroups = entity.getRequirementGroups();
        if (requirementGroups != null) {
            requirementGroupIds = requirementGroups.stream()
                    .map(RequirementGroup::getId)
                    .toList();
        }
    }
}
