package com.kevinguanchedarias.sgtjava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

/**
 * Has the special field secondValue , which represents the whole reason to
 * match ObjectRelation with a Requirement
 * 
 * @author Kevin Guanche Darias
 *
 */
@Entity
@Table(name = "requirements_information")
public class RequirementInformation implements SimpleIdEntity {
	private static final long serialVersionUID = -4898440527789250186L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "relation_id")
	@Fetch(FetchMode.JOIN)
	private ObjectRelation relation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "requirement_id")
	@Fetch(FetchMode.JOIN)
	private Requirement requirement;

	@Column(name = "second_value")
	private Long secondValue;

	@Column(name = "third_value")
	private Long thirdValue;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ObjectRelation getRelation() {
		return relation;
	}

	public void setRelation(ObjectRelation relationId) {
		this.relation = relationId;
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
