package com.kevinguanchedarias.owgejava.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.FactionUnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.ImageStore;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

public class FactionDto extends CommonDtoWithImageStore<Integer, Faction>
		implements WithDtoFromEntityTrait<Faction>, DtoWithImprovements {

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

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait#dtoFromEntity(
	 * java.lang.Object)
	 */
	@Override
	public void dtoFromEntity(Faction entity) {
		super.dtoFromEntity(entity);
		ImageStore primaryResourceImageEntity = entity.getPrimaryResourceImage();
		ImageStore secondaryResourceImageEntity = entity.getSecondaryResourceImage();
		ImageStore energyImageEntity = entity.getEnergyImage();
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

	/**
	 *
	 * @param faction
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void loadUnitTypesOverrides(Faction faction) {
		unitTypes = faction.getUnitTypes().stream().map(current -> {
			FactionUnitTypeDto dto = new FactionUnitTypeDto();
			dto.dtoFromEntity(current);
			return dto;
		}).collect(Collectors.toList());
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	public String getPrimaryResourceName() {
		return primaryResourceName;
	}

	public void setPrimaryResourceName(String primaryResourceName) {
		this.primaryResourceName = primaryResourceName;
	}

	public String getSecondaryResourceName() {
		return secondaryResourceName;
	}

	public void setSecondaryResourceName(String secondaryResourceName) {
		this.secondaryResourceName = secondaryResourceName;
	}

	public String getEnergyName() {
		return energyName;
	}

	public void setEnergyName(String energyName) {
		this.energyName = energyName;
	}

	public Integer getInitialPrimaryResource() {
		return initialPrimaryResource;
	}

	public void setInitialPrimaryResource(Integer initialPrimaryResource) {
		this.initialPrimaryResource = initialPrimaryResource;
	}

	public Integer getInitialSecondaryResource() {
		return initialSecondaryResource;
	}

	public void setInitialSecondaryResource(Integer initialSecondaryResource) {
		this.initialSecondaryResource = initialSecondaryResource;
	}

	public Integer getInitialEnergy() {
		return initialEnergy;
	}

	public void setInitialEnergy(Integer initialEnergy) {
		this.initialEnergy = initialEnergy;
	}

	public Float getPrimaryResourceProduction() {
		return primaryResourceProduction;
	}

	public void setPrimaryResourceProduction(Float primaryResourceProduction) {
		this.primaryResourceProduction = primaryResourceProduction;
	}

	public Float getSecondaryResourceProduction() {
		return secondaryResourceProduction;
	}

	public void setSecondaryResourceProduction(Float secondaryResourceProduction) {
		this.secondaryResourceProduction = secondaryResourceProduction;
	}

	public Boolean getClonedImprovements() {
		return clonedImprovements;
	}

	public void setClonedImprovements(Boolean clonedImprovements) {
		this.clonedImprovements = clonedImprovements;
	}

	/**
	 * @return the primaryResourceImage
	 * @since 0.9.0
	 */
	public Long getPrimaryResourceImage() {
		return primaryResourceImage;
	}

	/**
	 * @param primaryResourceImage the primaryResourceImage to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setPrimaryResourceImage(Long primaryResourceImage) {
		this.primaryResourceImage = primaryResourceImage;
	}

	/**
	 * @return the primaryResourceImageUrl
	 * @since 0.9.0
	 */
	public String getPrimaryResourceImageUrl() {
		return primaryResourceImageUrl;
	}

	/**
	 * @param primaryResourceImageUrl the primaryResourceImageUrl to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setPrimaryResourceImageUrl(String primaryResourceImageUrl) {
		this.primaryResourceImageUrl = primaryResourceImageUrl;
	}

	/**
	 * @return the secondaryResourceImage
	 * @since 0.9.0
	 */
	public Long getSecondaryResourceImage() {
		return secondaryResourceImage;
	}

	/**
	 * @param secondaryResourceImage the secondaryResourceImage to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSecondaryResourceImage(Long secondaryResourceImage) {
		this.secondaryResourceImage = secondaryResourceImage;
	}

	/**
	 * @return the secondaryResourceImageUrl
	 * @since 0.9.0
	 */
	public String getSecondaryResourceImageUrl() {
		return secondaryResourceImageUrl;
	}

	/**
	 * @param secondaryResourceImageUrl the secondaryResourceImageUrl to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSecondaryResourceImageUrl(String secondaryResourceImageUrl) {
		this.secondaryResourceImageUrl = secondaryResourceImageUrl;
	}

	/**
	 * @return the energyImage
	 * @since 0.9.0
	 */
	public Long getEnergyImage() {
		return energyImage;
	}

	/**
	 * @param energyImage the energyImage to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEnergyImage(Long energyImage) {
		this.energyImage = energyImage;
	}

	/**
	 * @return the energyImageUrl
	 * @since 0.9.0
	 */
	public String getEnergyImageUrl() {
		return energyImageUrl;
	}

	/**
	 * @param energyImageUrl the energyImageUrl to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEnergyImageUrl(String energyImageUrl) {
		this.energyImageUrl = energyImageUrl;
	}

	public Integer getMaxPlanets() {
		return maxPlanets;
	}

	public void setMaxPlanets(Integer maxPlanets) {
		this.maxPlanets = maxPlanets;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.dto.DtoWithImprovements#getImprovement()
	 */
	@Override
	public ImprovementDto getImprovement() {
		return improvement;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.kevinguanchedarias.owgejava.dto.DtoWithImprovements#setImprovement(com.
	 * kevinguanchedarias.owgejava.dto.ImprovementDto)
	 */
	@Override
	public void setImprovement(ImprovementDto improvementDto) {
		improvement = improvementDto;
	}

	/**
	 * @return the customPrimaryGatherPercentage
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getCustomPrimaryGatherPercentage() {
		return customPrimaryGatherPercentage;
	}

	/**
	 * @param customPrimaryGatherPercentage the customPrimaryGatherPercentage to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setCustomPrimaryGatherPercentage(Float customPrimaryGatherPercentage) {
		this.customPrimaryGatherPercentage = customPrimaryGatherPercentage;
	}

	/**
	 * @return the customSecondaryGatherPercentage
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getCustomSecondaryGatherPercentage() {
		return customSecondaryGatherPercentage;
	}

	/**
	 * @param customSecondaryGatherPercentage the customSecondaryGatherPercentage to
	 *                                        set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setCustomSecondaryGatherPercentage(Float customSecondaryGatherPercentage) {
		this.customSecondaryGatherPercentage = customSecondaryGatherPercentage;
	}

	/**
	 * @return the unitTypes
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<FactionUnitTypeDto> getUnitTypes() {
		return unitTypes;
	}

	/**
	 * @param unitTypes the unitTypes to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setUnitTypes(List<FactionUnitTypeDto> unitTypes) {
		this.unitTypes = unitTypes;
	}

}
