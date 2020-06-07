package com.kevinguanchedarias.owgejava.business;

import java.util.List;
import java.util.stream.Collectors;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 * @param <E>
 * @param <D>
 */
public interface WithToDtoTrait<E, D extends DtoFromEntity<E>> {

	/**
	 *
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	Class<D> getDtoClass();

	/**
	 * Converts the entity to a DTO
	 *
	 * @param entity
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default D toDto(E entity) {
		if (entity == null) {
			return null;
		} else {
			return DtoUtilService.staticDtoFromEntity(getDtoClass(), entity);
		}
	}

	/**
	 * Converts an entire entity list to DTO list
	 *
	 * @param entities
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default List<D> toDto(List<E> entities) {
		return entities.stream().map(this::toDto).collect(Collectors.toList());
	}
}
