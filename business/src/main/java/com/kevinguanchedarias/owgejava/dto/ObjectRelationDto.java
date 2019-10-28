/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 * DTO for {@link ObjectRelation}
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class ObjectRelationDto implements WithDtoFromEntityTrait<ObjectRelation> {
	private Integer id;
	private String objectCode;
	private Integer referenceId;

	/**
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectRelationDto() {

	}

	/**
	 * @param objectCode
	 * @param referenceId
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectRelationDto(String objectCode, Integer referenceId) {
		super();
		this.objectCode = objectCode;
		this.referenceId = referenceId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait#dtoFromEntity(
	 * java.lang.Object)
	 */
	@Override
	public void dtoFromEntity(ObjectRelation entity) {
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
		objectCode = entity.getObject().getCode();
	}

	/**
	 * @since 0.8.0
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @since 0.8.0
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @since 0.8.0
	 * @return the objectCode
	 */
	public String getObjectCode() {
		return objectCode;
	}

	/**
	 * @since 0.8.0
	 * @param objectCode the objectCode to set
	 */
	public void setObjectCode(String objectCode) {
		this.objectCode = objectCode;
	}

	/**
	 * @since 0.8.0
	 * @return the referenceId
	 */
	public Integer getReferenceId() {
		return referenceId;
	}

	/**
	 * @since 0.8.0
	 * @param referenceId the referenceId to set
	 */
	public void setReferenceId(Integer referenceId) {
		this.referenceId = referenceId;
	}

}
