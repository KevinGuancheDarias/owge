package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Has the special field secondValue , which represents the whole reason to
 * match ObjectRelation with a Requirement
 *
 * @author Kevin Guanche Darias
 *
 */
@Entity
@Table(name = "requirements_information")
public class RequirementInformation implements EntityWithId<Integer> {
	private static final long serialVersionUID = -4898440527789250186L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "relation_id")
	private ObjectRelation relation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "requirement_id")
	private Requirement requirement;

	@Column(name = "second_value")
	private Long secondValue;

	@Column(name = "third_value")
	private Long thirdValue;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public ObjectRelation getRelation() {
		return relation;
	}

	public void setRelation(ObjectRelation relationId) {
		relation = relationId;
	}

	public Requirement getRequirement() {
		return requirement;
	}

	public void setRequirement(Requirement requirement) {
		this.requirement = requirement;
	}

	public Long getSecondValue() {
		return secondValue;
	}

	public void setSecondValue(Long secondValue) {
		this.secondValue = secondValue;
	}

	public Long getThirdValue() {
		return thirdValue;
	}

	public void setThirdValue(Long thirdValue) {
		this.thirdValue = thirdValue;
	}
}
