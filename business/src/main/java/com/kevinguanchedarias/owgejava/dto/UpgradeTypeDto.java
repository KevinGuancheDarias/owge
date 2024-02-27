package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.UpgradeType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpgradeTypeDto implements DtoFromEntity<UpgradeType> {

    private Integer id;
    private String name;

    @Override
    public void dtoFromEntity(UpgradeType entity) {
        id = entity.getId();
        name = entity.getName();
    }

}
