package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ImprovementDto extends AbstractImprovementDto implements DtoFromEntity<Improvement> {
    @EqualsAndHashCode.Include
    private Integer id;

    @Override
    public void dtoFromEntity(Improvement entity) {
        id = entity.getId();
        super.dtoFromEntity(entity);
    }
}
