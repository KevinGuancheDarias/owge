package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.FactionUnitType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 */
@Data
@NoArgsConstructor
public class FactionUnitTypeDto implements DtoFromEntity<FactionUnitType> {

    private Integer id;
    private Integer factionId;
    private Integer unitTypeId;
    private Long maxCount;

    @Override
    public void dtoFromEntity(FactionUnitType entity) {
        id = entity.getId();
        factionId = entity.getFaction().getId();
        unitTypeId = entity.getUnitType().getId();
        maxCount = entity.getMaxCount();
    }
}
