/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.Optional;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface CrudWithFullRestService<N extends Number, E extends EntityWithImprovements<N>, S extends BaseBo<N, E, D>, D extends DtoFromEntity<E>>
		extends CrudWithRequirementsRestServiceTrait<N, E, S, D>, CrudWithImprovementsRestServiceTrait<N, E, S, D>,
		WithReadRestServiceTrait<N, E, S, D>, CrudRestServiceTrait<N, E, S, D>, WithDeleteRestServiceTrait<N, E, S, D> {

	@Override
	public default Optional<D> beforeRequestEnd(D dto, E savedEntity) {
		return CrudWithImprovementsRestServiceTrait.super.beforeRequestEnd(dto, savedEntity);
	}

}
