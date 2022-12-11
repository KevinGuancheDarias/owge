package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementGroupDto implements DtoFromEntity<RequirementGroup> {

    private Integer id;
    private String name;
    private List<RequirementInformationDto> requirements;

    @Override
    public void dtoFromEntity(RequirementGroup entity) {
        id = entity.getId();
        name = entity.getName();
        if (entity.getRequirements() != null) {
            requirements = entity.getRequirements().stream().map(current -> {
                RequirementInformationDto dto = new RequirementInformationDto();
                dto.dtoFromEntity(current);
                return dto;
            }).toList();
        }
    }
}
