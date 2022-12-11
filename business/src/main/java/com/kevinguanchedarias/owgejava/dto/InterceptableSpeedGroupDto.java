package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
import lombok.Data;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 */
@Data
public class InterceptableSpeedGroupDto implements DtoFromEntity<InterceptableSpeedGroup> {
    private Integer id;
    private UnitDto unit;
    private SpeedImpactGroupDto speedImpactGroup;

    @Override
    public void dtoFromEntity(InterceptableSpeedGroup interceptableSpeedGroup) {
        id = interceptableSpeedGroup.getId();
        speedImpactGroup = new SpeedImpactGroupDto();
        speedImpactGroup.dtoFromEntity(interceptableSpeedGroup.getSpeedImpactGroup());
    }
}
