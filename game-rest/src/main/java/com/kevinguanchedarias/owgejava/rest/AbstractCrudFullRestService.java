/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.business.ObjectEntityBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.RequirementInformationBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithRequirementsRestServiceTrait;

/**
 * This Crud has all CRUD operations
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractCrudFullRestService<N extends Number, E extends EntityWithImprovements, S extends BaseBo<E>, D extends DtoFromEntity<E>>
		extends AbstractCrudWithImprovementsRestService<N, E, S, D>
		implements CrudWithRequirementsRestServiceTrait<N, E, S> {

	@Autowired
	protected RequirementBo requirementBo;

	@Autowired
	protected ObjectEntityBo objectEntityBo;

	@Autowired
	protected RequirementInformationBo requirementInformationBo;

	@Override
	public RequirementBo getRequirementBo() {
		return requirementBo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * CrudWithRequirementsRestServiceTrait#getRequirementInformationBo()
	 */
	@Override
	public RequirementInformationBo getRequirementInformationBo() {
		return requirementInformationBo;
	}

	@Override
	public ObjectEntityBo getObjectEntityBo() {
		return objectEntityBo;
	}
}
