package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UpgradeType;

public class UpgradeDto extends CommonDtoWithImageStore<Integer, Upgrade> implements DtoWithImprovements {
	private Integer points = 0;
	private Long time = 60L;
	private Integer primaryResource = 100;
	private Integer secondaryResource = 100;
	private Integer typeId;
	private String typeName;
	private Float levelEffect = 20f;
	private ImprovementDto improvement;
	private Boolean clonedImprovements = false;

	@Override
	public void dtoFromEntity(Upgrade entity) {
		super.dtoFromEntity(entity);
		UpgradeType typeEntity = entity.getType();
		typeId = typeEntity.getId();
		typeName = typeEntity.getName();
		DtoWithImprovements.super.dtoFromEntity(entity);
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Integer getPrimaryResource() {
		return primaryResource;
	}

	public void setPrimaryResource(Integer primaryResource) {
		this.primaryResource = primaryResource;
	}

	public Integer getSecondaryResource() {
		return secondaryResource;
	}

	public void setSecondaryResource(Integer secondaryResource) {
		this.secondaryResource = secondaryResource;
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public Float getLevelEffect() {
		return levelEffect;
	}

	public void setLevelEffect(Float levelEffect) {
		this.levelEffect = levelEffect;
	}

	@Override
	public ImprovementDto getImprovement() {
		return improvement;
	}

	@Override
	public void setImprovement(ImprovementDto improvement) {
		this.improvement = improvement;
	}

	public Boolean getClonedImprovements() {
		return clonedImprovements;
	}

	public void setClonedImprovements(Boolean clonedImprovements) {
		this.clonedImprovements = clonedImprovements;
	}

}
