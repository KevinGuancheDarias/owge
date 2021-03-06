package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class UnitDto extends CommonDtoWithImageStore<Integer, Unit> implements DtoWithImprovements {
	private Boolean hasToDisplayInRequirements;
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
	private Boolean canFastExplore = false;
	private Double speed;
	private ImprovementDto improvement;
	private Boolean clonedImprovements = false;
	private SpeedImpactGroupDto speedImpactGroup;
	private AttackRuleDto attackRule;

	@Getter
	@Setter
	private CriticalAttackDto criticalAttack;
	

	private Boolean bypassShield = false;
	private Boolean isInvisible = false;
	private List<RequirementInformationDto> requirements;
	private List<InterceptableSpeedGroupDto> interceptableSpeedGroups;

	@Override
	public void dtoFromEntity(Unit entity) {
		super.dtoFromEntity(entity);
		interceptableSpeedGroups = null;
		UnitType typeEntity = entity.getType();
		typeId = typeEntity.getId();
		typeName = typeEntity.getName();
		if (entity.getSpeedImpactGroup() != null) {
			speedImpactGroup = new SpeedImpactGroupDto();
			speedImpactGroup.dtoFromEntity(entity.getSpeedImpactGroup());
		}
		if (entity.getAttackRule() != null) {
			attackRule = new AttackRuleDto();
			attackRule.dtoFromEntity(entity.getAttackRule());
		}
		var criticalAttackEntity = entity.getCriticalAttack();
		if (criticalAttackEntity != null) {
			criticalAttack = new CriticalAttackDto();
			criticalAttack.dtoFromEntity(criticalAttackEntity);
			criticalAttack.setEntries(DtoUtilService.staticDtosFromEntities(CriticalAttackEntryDto.class, criticalAttackEntity.getEntries()));
		}
		List<InterceptableSpeedGroup> interceptableSpeedGroupsEntity = entity.getInterceptableSpeedGroups();
		if (Hibernate.isInitialized(interceptableSpeedGroupsEntity)
				&& !CollectionUtils.isEmpty(interceptableSpeedGroupsEntity)) {
			interceptableSpeedGroups = interceptableSpeedGroupsEntity.stream().map(current -> {
				var dto = new InterceptableSpeedGroupDto();
				dto.dtoFromEntity(current);
				return dto;
			}).collect(Collectors.toList());
		}
		DtoWithImprovements.super.dtoFromEntity(entity);
	}

	/**
	 * @return the hasToDisplayInRequirements
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getHasToDisplayInRequirements() {
		return Boolean.TRUE.equals(hasToDisplayInRequirements);
	}

	/**
	 * @param hasToDisplayInRequirements the hasToDisplayInRequirements to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setHasToDisplayInRequirements(Boolean hasToDisplayInRequirements) {
		this.hasToDisplayInRequirements = hasToDisplayInRequirements;
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

	/**
	 * @return the canFastExplore
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getCanFastExplore() {
		return canFastExplore;
	}

	/**
	 * @param canFastExplore the canFastExplore to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanFastExplore(Boolean canFastExplore) {
		this.canFastExplore = canFastExplore;
	}

	/**
	 * @return the speed
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double getSpeed() {
		return speed;
	}

	/**
	 * @param speed the speed to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSpeed(Double speed) {
		this.speed = speed;
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

	/**
	 * @return the bypassShield
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getBypassShield() {
		return bypassShield;
	}

	/**
	 * @param bypassShield the bypassShield to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setBypassShield(Boolean bypassShield) {
		this.bypassShield = bypassShield;
	}

	/**
	 * @return the isInvisible
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getIsInvisible() {
		return isInvisible;
	}

	/**
	 * @param isInvisible the isInvisible to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setIsInvisible(Boolean isInvisible) {
		this.isInvisible = isInvisible;
	}

	/**
	 * @return the requirements
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<RequirementInformationDto> getRequirements() {
		return requirements;
	}

	/**
	 * @param requirements the requirements to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setRequirements(List<RequirementInformationDto> requirements) {
		this.requirements = requirements;
	}

	/**
	 * @return the interceptableSpeedGroups
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<InterceptableSpeedGroupDto> getInterceptableSpeedGroups() {
		return interceptableSpeedGroups;
	}

	/**
	 * @param interceptableSpeedGroups the interceptableSpeedGroups to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setInterceptableSpeedGroups(List<InterceptableSpeedGroupDto> interceptableSpeedGroups) {
		this.interceptableSpeedGroups = interceptableSpeedGroups;
	}

}
