package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;

public class UnitDto extends CommonDtoWithImageStore<Integer, Unit> implements DtoWithImprovements {
	private Integer points = 0;
	private Integer time = 60;
	private Integer primaryResource = 100;
	private Integer secondaryResource = 100;
	private Integer energy;
	private Integer typeId;
	private String typeName;
	private Integer attack;
	private Integer health = 1;
	private Integer shield;
	private Integer charge;
	private Boolean isUnique = false;
	private ImprovementDto improvement;
	private Boolean clonedImprovements = false;

	@Override
	public void dtoFromEntity(Unit entity) {
		super.dtoFromEntity(entity);
		UnitType typeEntity = entity.getType();
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

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
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

	public Integer getEnergy() {
		return energy;
	}

	public void setEnergy(Integer energy) {
		this.energy = energy;
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

	public Integer getAttack() {
		return attack;
	}

	public void setAttack(Integer attack) {
		this.attack = attack;
	}

	public Integer getHealth() {
		return health;
	}

	public void setHealth(Integer health) {
		this.health = health;
	}

	public Integer getShield() {
		return shield;
	}

	public void setShield(Integer shield) {
		this.shield = shield;
	}

	public Integer getCharge() {
		return charge;
	}

	public void setCharge(Integer charge) {
		this.charge = charge;
	}

	public Boolean getIsUnique() {
		return isUnique;
	}

	public void setIsUnique(Boolean isUnique) {
		this.isUnique = isUnique;
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
