package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ObtainedUpgradeDto implements DtoFromEntity<ObtainedUpgrade> {
    @EqualsAndHashCode.Include
    private Long id;

    private Integer level;
    private Boolean available;
    private UpgradeDto upgrade;

    @Override
    public void dtoFromEntity(ObtainedUpgrade entity) {
        id = entity.getId();
        level = entity.getLevel();
        available = Boolean.TRUE.equals(entity.getAvailable());
        upgrade = new UpgradeDto();
        upgrade.dtoFromEntity(entity.getUpgrade());
    }
}
