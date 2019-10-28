package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UpgradeType;

public class UpgradeDto implements DtoFromEntity<Upgrade>, DtoWithImprovements {
	private Integer id;
	private String name;
	private String image;
	private Integer points = 0;
	private String description;
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
		EntityPojoConverterUtil.convertFromTo(this, entity);
		UpgradeType typeEntity = entity.getType();
		typeId = typeEntity.getId();
		typeName = typeEntity.getName();

		DtoWithImprovements.super.dtoFromEntity(entity);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
