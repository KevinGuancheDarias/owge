package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Upgrade;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UpgradeDto extends CommonDtoWithImageStore<Integer, Upgrade> implements DtoWithImprovements {
    private Integer points;
    private Long time;
    private Integer primaryResource;
    private Integer secondaryResource;
    private Integer typeId;
    private String typeName;
    private Float levelEffect;
    private ImprovementDto improvement;
    private Boolean clonedImprovements = false;
    private List<RequirementInformationDto> requirements;

    @Override
    public void dtoFromEntity(Upgrade entity) {
        loadData(entity);
        super.dtoFromEntity(entity);
        var typeEntity = entity.getType();
        typeId = typeEntity.getId();
        typeName = typeEntity.getName();
        DtoWithImprovements.super.dtoFromEntity(entity);
        if (entity.getRequirements() != null) {
            requirements = entity.getRequirements().stream().map(current -> {
                var dto = new RequirementInformationDto();
                dto.dtoFromEntity(current);
                return dto;
            }).toList();
        }
    }

    private void loadData(Upgrade entity) {
        points = entity.getPoints();
        time = entity.getTime();
        primaryResource = entity.getPrimaryResource();
        secondaryResource = entity.getSecondaryResource();
        levelEffect = entity.getLevelEffect();
        clonedImprovements = Boolean.TRUE.equals(entity.getClonedImprovements());
    }
}
