package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class AttackRuleEntryDto implements WithDtoFromEntityTrait<AttackRuleEntry> {
	private Integer id;
	private AttackableTargetEnum target;
	private Integer referenceId;
	private Boolean canAttack = false;
	private String referenceName;

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the target
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public AttackableTargetEnum getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setTarget(AttackableTargetEnum target) {
		this.target = target;
	}

	/**
	 * @return the referenceId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getReferenceId() {
		return referenceId;
	}

	/**
	 * @param referenceId the referenceId to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setReferenceId(Integer referenceId) {
		this.referenceId = referenceId;
	}

	/**
	 * @return the canAttack
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getCanAttack() {
		return canAttack;
	}

	/**
	 * @param canAttack the canAttack to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanAttack(Boolean canAttack) {
		this.canAttack = canAttack;
	}

	/**
	 * @return the referenceName
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getReferenceName() {
		return referenceName;
	}

	/**
	 * @param referenceName the referenceName to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setReferenceName(String referenceName) {
		this.referenceName = referenceName;
	}

}
