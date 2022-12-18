package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Faction;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactionDto extends CommonDtoWithImageStore<Integer, Faction> implements DtoWithImprovements {

    @EqualsAndHashCode.Include
    private Integer id;

    private Boolean hidden;
    private String name;
    private String description;
    private String primaryResourceName;
    private Long primaryResourceImage;
    private String primaryResourceImageUrl;
    private String secondaryResourceName;
    private Long secondaryResourceImage;
    private String secondaryResourceImageUrl;
    private String energyName;
    private Long energyImage;
    private String energyImageUrl;
    private Integer initialPrimaryResource;
    private Integer initialSecondaryResource;
    private Integer initialEnergy;
    private Float primaryResourceProduction;
    private Float secondaryResourceProduction;
    private Integer maxPlanets;
    private Boolean clonedImprovements = false;
    private ImprovementDto improvement;
    private Float customPrimaryGatherPercentage = 0F;
    private Float customSecondaryGatherPercentage = 0F;
    private List<FactionUnitTypeDto> unitTypes;

    @Override
    public void dtoFromEntity(Faction entity) {
        loadBaseInfo(entity);
        super.dtoFromEntity(entity);
        unitTypes = null;
        var primaryResourceImageEntity = entity.getPrimaryResourceImage();
        var secondaryResourceImageEntity = entity.getSecondaryResourceImage();
        var energyImageEntity = entity.getEnergyImage();
        if (primaryResourceImageEntity != null) {
            primaryResourceImage = primaryResourceImageEntity.getId();
            primaryResourceImageUrl = primaryResourceImageEntity.getUrl();
        }
        if (secondaryResourceImageEntity != null) {
            secondaryResourceImage = secondaryResourceImageEntity.getId();
            secondaryResourceImageUrl = secondaryResourceImageEntity.getUrl();
        }
        if (energyImageEntity != null) {
            energyImage = energyImageEntity.getId();
            energyImageUrl = energyImageEntity.getUrl();
        }
        DtoWithImprovements.super.dtoFromEntity(entity);
    }

    private void loadBaseInfo(Faction entity) {
        id = entity.getId();
        hidden = entity.getHidden();
        name = entity.getName();
        description = entity.getDescription();
        primaryResourceName = entity.getPrimaryResourceName();
        secondaryResourceName = entity.getSecondaryResourceName();
        energyName = entity.getEnergyName();
        initialPrimaryResource = entity.getInitialPrimaryResource();
        initialSecondaryResource = entity.getInitialSecondaryResource();
        initialEnergy = entity.getInitialEnergy();
        primaryResourceProduction = entity.getPrimaryResourceProduction();
        secondaryResourceProduction = entity.getSecondaryResourceProduction();
        maxPlanets = entity.getMaxPlanets();
        clonedImprovements = entity.getClonedImprovements();
        customPrimaryGatherPercentage = entity.getCustomPrimaryGatherPercentage();
        customSecondaryGatherPercentage = entity.getCustomSecondaryGatherPercentage();
    }
}
