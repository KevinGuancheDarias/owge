package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.entity.Improvement;

import java.util.ArrayList;

public class ImprovementDto extends AbstractImprovementDto implements DtoFromEntity<Improvement> {
    private Integer id;

    @Override
    public void dtoFromEntity(Improvement entity) {
        EntityPojoConverterUtil.convertFromTo(this, entity);
        setUnitTypesUpgrades(new ArrayList<>());
        if (entity.getUnitTypesUpgrades() != null) {
            entity.getUnitTypesUpgrades().forEach(current -> {
                ImprovementUnitTypeDto currentDto = new ImprovementUnitTypeDto();
                currentDto.dtoFromEntity(current);
                getUnitTypesUpgrades().add(currentDto);
            });
        }
    }
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
