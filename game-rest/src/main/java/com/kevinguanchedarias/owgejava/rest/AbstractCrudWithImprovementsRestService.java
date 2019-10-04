/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.ImprovementUnitTypeBo;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithImprovementsRestServiceTrait;

/**
 * Extend this class to save some lines when using
 * {@link CrudWithImprovementsRestServiceTrait}
 *
 * @param <N> Entity <b>Numeric</b> id type
 * @param <E> Entity class
 * @param <S> Business service used for crud operations
 * @param <D> DTO class used to build the response, or to build the
 *            "RequestBody" object for POST PUT crud operations
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractCrudWithImprovementsRestService<N extends Number, E extends EntityWithImprovements, S extends BaseBo<E>, D extends DtoFromEntity<E>>
		extends AbstractCrudRestService<N, E, S, D> implements CrudWithImprovementsRestServiceTrait<N, E, S, D> {

	@Autowired
	protected ImprovementBo improvementBo;

	@Autowired
	protected ImprovementUnitTypeBo improvementUnitTypeBo;

	@Autowired
	protected UnitTypeBo unitTypeBo;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * AdminCrudWithImprovementsRestServiceTrait#getImprovementBo()
	 */
	@Override
	public ImprovementBo getImprovementBo() {
		return improvementBo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * AdminCrudWithImprovementsRestServiceTrait#getImprovementUnitTypeBo()
	 */
	@Override
	public ImprovementUnitTypeBo getImprovementUnitTypeBo() {
		return improvementUnitTypeBo;
	}

	/**
	 * @since 0.8.0
	 * @return the unitTypeBo
	 */
	@Override
	public UnitTypeBo getUnitTypeBo() {
		return unitTypeBo;
	}
}
