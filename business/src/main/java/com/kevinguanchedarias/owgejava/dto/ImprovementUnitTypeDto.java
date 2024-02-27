package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;
import lombok.Data;

@Data
public class ImprovementUnitTypeDto implements DtoFromEntity<ImprovementUnitType> {
    private Integer id;
    private String type;
    private Integer unitTypeId;
    private String unitTypeName;
    private UnitTypeDto unitType;
    private Long value;

    @Override
    public void dtoFromEntity(ImprovementUnitType entity) {
        id = entity.getId();
        type = entity.getType();
        if (entity.getUnitType() != null) {
            unitType = new UnitTypeDto();
            unitType.dtoFromEntity(entity.getUnitType());
        }
        value = entity.getValue();
    }
}
