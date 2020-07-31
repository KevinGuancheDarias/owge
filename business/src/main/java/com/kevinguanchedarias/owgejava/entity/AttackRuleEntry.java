package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "attack_rule_entries")
public class AttackRuleEntry {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "attack_rule_id")
	private AttackRule attackRule;

	@Enumerated(EnumType.STRING)
	private AttackableTargetEnum target;

	@Column(name = "reference_id")
	private Integer referenceId;

	@Column(name = "can_attack")
	private Boolean canAttack = false;

	@Transient
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
	 * @return the attackRule
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public AttackRule getAttackRule() {
		return attackRule;
	}

	/**
	 * @param attackRule the attackRule to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setAttackRule(AttackRule attackRule) {
		this.attackRule = attackRule;
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
