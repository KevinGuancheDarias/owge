package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

import java.util.List;
import java.util.stream.Collectors;

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
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	Class<D> getDtoClass();

	/**
	 * Converts the entity to a DTO
	 *
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
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default E save(D dto) {
		throw new SgtBackendNotImplementedException("This bo service doesn't have a save from DTO");
	}

	/**
	 * Converts an entire entity list to DTO list
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default List<D> toDto(List<E> entities) {
		return entities.stream().map(this::toDto).collect(Collectors.toList());
	}
}
