package com.kevinguanchedarias.owgejava.dto;

public class FactionDto {

	private Integer id;
	private Boolean hidden;
	private String name;
	private String description;
	private String primaryResourceName;
	private String primaryResourceImage;
	private String secondaryResourceName;
	private String secondaryResourceImage;
	private String energyName;
	private String energyImage;
	private Integer initialPrimaryResource;
	private Integer initialSecondaryResource;
	private Integer initialEnergy;
	private Float primaryResourceProduction;
	private Float secondaryResourceProduction;
	private Integer maxPlanets;
	private Boolean clonedImprovements = false;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

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

	public String getPrimaryResourceImage() {
		return primaryResourceImage;
	}

	public void setPrimaryResourceImage(String primaryResourceImage) {
		this.primaryResourceImage = primaryResourceImage;
	}

	public String getSecondaryResourceImage() {
		return secondaryResourceImage;
	}

	public void setSecondaryResourceImage(String secondaryResourceImage) {
		this.secondaryResourceImage = secondaryResourceImage;
	}

	public String getEnergyImage() {
		return energyImage;
	}

	public void setEnergyImage(String energyImage) {
		this.energyImage = energyImage;
	}

	public Integer getMaxPlanets() {
		return maxPlanets;
	}

	public void setMaxPlanets(Integer maxPlanets) {
		this.maxPlanets = maxPlanets;
	}
}
