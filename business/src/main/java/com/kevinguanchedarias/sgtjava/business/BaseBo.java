package com.kevinguanchedarias.sgtjava.business;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;
import com.kevinguanchedarias.sgtjava.entity.SpecialLocation;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendEntityNotFoundException;

@FunctionalInterface
public interface BaseBo<E extends SimpleIdEntity> extends Serializable {
	public abstract JpaRepository<E, Number> getRepository();

	public default List<E> findAll() {
		return getRepository().findAll();
	}

	public default Long countAll() {
		return getRepository().count();
	}

	public default E findById(Number id) {
		return getRepository().findOne(id);
	}

	public default E findByIdOrDie(Number id) {
		E retVal = getRepository().findOne(id);
		if (retVal == null) {
			throw new SgtBackendEntityNotFoundException("No entyty with id " + id + " found for repository "
					+ getRepository().getClass().getCanonicalName());
		}
		return retVal;
	}

	public default E save(E entity) {
		return getRepository().save(entity);
	}

	public default void save(List<E> entities) {
		getRepository().save(entities);
	}

	public default E saveAndFlush(E entity) {
		return getRepository().saveAndFlush(entity);
	}

	public default void delete(E entity) {
		getRepository().delete(entity);
	}

	public default boolean exists(E entity) {
		return exists(entity.getId());
	}

	public default boolean exists(Number id) {
		return getRepository().exists(id);
	}

	/**
	 * Used to refresh the entity for example for lazy fetching
	 * FETCH-Joins<br />
	 * This method is useful for example to refresh {@link SpecialLocation}
	 * galaxy
	 * 
	 * @param entity
	 *            Notice: doesn't refresh the source entity
	 * @return The refreshed entity
	 * @author Kevin Guanche Darias
	 */
	public default E refresh(E entity) {
		return getRepository().findOne(entity.getId());
	}
}
