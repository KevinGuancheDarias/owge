/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;
import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 * Extend this class to save some lines when using {@link CrudRestServiceTrait}
 *
 * @param <N> Entity <b>Numeric</b> id type
 * @param <E> Entity class
 * @param <S> Business service used for crud operations
 * @param <D> DTO class used to build the response, or to build the
 *            "RequestBody" object for POST PUT crud operations
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractCrudRestService<N extends Number, E extends SimpleIdEntity, S extends BaseBo<E>, D extends DtoFromEntity<E>>
		implements CrudRestServiceTrait<N, E, S, D> {

	@Autowired
	protected DtoUtilService dtoUtilService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.AdminCrudRestServiceTrait#
	 * getDtoUtilService()
	 */
	@Override
	public DtoUtilService getDtoUtilService() {
		return dtoUtilService;
	}
}
