package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.dto.base.DtoWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.UnitType;

public class UnitTypeDto extends DtoWithMissionLimitation implements DtoFromEntity<UnitType> {

	private Integer id;
	private String name;
	private Long image;
	private String imageUrl;
	private Long maxCount;
	private Long computedMaxCount;
	private Long userBuilt;
	private SpeedImpactGroupDto speedImpactGroup;
	private AttackRuleDto attackRule;

	@Override
	public void dtoFromEntity(UnitType entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
		if (entity.getImage() != null) {
			image = entity.getImage().getId();
			imageUrl = entity.getImage().getUrl();
		}
		if (entity.getSpeedImpactGroup() != null) {
			speedImpactGroup = new SpeedImpactGroupDto();
			speedImpactGroup.dtoFromEntity(entity.getSpeedImpactGroup());
		}
		if (entity.getAttackRule() != null) {
			attackRule = new AttackRuleDto();
			attackRule.dtoFromEntity(entity.getAttackRule());
		}
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

	public Long getImage() {
		return image;
	}

	public void setImage(Long image) {
		this.image = image;
	}

	/**
	 * @return the imageUrl
	 * @author Kevin Guanche Darias
	 * @since 0.8.1
	 */
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * @param imageUrl the imageUrl to set
	 * @author Kevin Guanche Darias
	 * @since 0.8.1
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
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

	/**
	 * @return the speedImpactGroup
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SpeedImpactGroupDto getSpeedImpactGroup() {
		return speedImpactGroup;
	}

	/**
	 * @param speedImpactGroup the speedImpactGroup to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSpeedImpactGroup(SpeedImpactGroupDto speedImpactGroup) {
		this.speedImpactGroup = speedImpactGroup;
	}

	/**
	 * @return the attackRule
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public AttackRuleDto getAttackRule() {
		return attackRule;
	}

	/**
	 * @param attackRule the attackRule to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setAttackRule(AttackRuleDto attackRule) {
		this.attackRule = attackRule;
	}

}
