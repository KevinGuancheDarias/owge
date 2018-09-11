package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.enumerations.MissionSupportEnum;

public class UnitTypeDto implements DtoFromEntity<UnitType> {

	private Integer id;
	private String name;
	private String image;
	private Long maxCount;
	private Long computedMaxCount;
	private Long userBuilt;
	private MissionSupportEnum canExplore = MissionSupportEnum.ANY;
	private MissionSupportEnum canGather = MissionSupportEnum.ANY;
	private MissionSupportEnum canEstablishBase = MissionSupportEnum.ANY;
	private MissionSupportEnum canAttack = MissionSupportEnum.ANY;
	private MissionSupportEnum canCounterattack = MissionSupportEnum.ANY;
	private MissionSupportEnum canConquest = MissionSupportEnum.ANY;
	private MissionSupportEnum canDeploy = MissionSupportEnum.ANY;

	@Override
	public void dtoFromEntity(UnitType entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
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

	public Long getMaxCount() {
		return maxCount;
	}

	public void setMaxCount(Long maxCount) {
		this.maxCount = maxCount;
	}

	/**
	 * Transient property, when defined, represents the maxCount with all user
	 * improvements applied
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getComputedMaxCount() {
		return computedMaxCount;
	}

	public void setComputedMaxCount(Long computedMaxCount) {
		this.computedMaxCount = computedMaxCount;
	}

	/**
	 * Represents the amount built by the user
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getUserBuilt() {
		return userBuilt;
	}

	public void setUserBuilt(Long userBuilt) {
		this.userBuilt = userBuilt;
	}

	public MissionSupportEnum getCanExplore() {
		return canExplore;
	}

	public void setCanExplore(MissionSupportEnum canExplore) {
		this.canExplore = canExplore;
	}

	public MissionSupportEnum getCanGather() {
		return canGather;
	}

	public void setCanGather(MissionSupportEnum canGather) {
		this.canGather = canGather;
	}

	public MissionSupportEnum getCanEstablishBase() {
		return canEstablishBase;
	}

	public void setCanEstablishBase(MissionSupportEnum canEstablishBase) {
		this.canEstablishBase = canEstablishBase;
	}

	public MissionSupportEnum getCanAttack() {
		return canAttack;
	}

	public void setCanAttack(MissionSupportEnum canAttack) {
		this.canAttack = canAttack;
	}

	public MissionSupportEnum getCanCounterattack() {
		return canCounterattack;
	}

	public void setCanCounterattack(MissionSupportEnum canCounterattack) {
		this.canCounterattack = canCounterattack;
	}

	public MissionSupportEnum getCanConquest() {
		return canConquest;
	}

	public void setCanConquest(MissionSupportEnum canConquest) {
		this.canConquest = canConquest;
	}

	public MissionSupportEnum getCanDeploy() {
		return canDeploy;
	}

	public void setCanDeploy(MissionSupportEnum canDeploy) {
		this.canDeploy = canDeploy;
	}

}
