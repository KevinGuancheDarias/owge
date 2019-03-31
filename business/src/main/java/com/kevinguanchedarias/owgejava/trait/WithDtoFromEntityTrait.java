/**
 * 
 */
package com.kevinguanchedarias.owgejava.trait;

import org.springframework.beans.BeanUtils;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;

/**
 * Copies common properties by default when invoking <i>dtoFromEntity</i>
 *
 * @param <E>
 *            Target entity
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface WithDtoFromEntityTrait<E> extends DtoFromEntity<E> {

	@Override
	public default void dtoFromEntity(E entity) {
		BeanUtils.copyProperties(entity, this);
	}
}
