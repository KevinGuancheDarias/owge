package com.kevinguanchedarias.sgtjava.business;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.SpecialLocation;

@FunctionalInterface
public interface BaseBo<E extends Serializable> extends Serializable {
	public abstract JpaRepository<E, Number> getRepository();

	public default List<E> findAll() {
		return getRepository().findAll();
	}

	public default E findById(Number id) {
		return getRepository().findOne(id);
	}

	public default E save(E entity) {
		return getRepository().save(entity);
	}

	public default E saveAndFlush(E entity) {
		return getRepository().saveAndFlush(entity);
	}

	public default void delete(E entity) {
		getRepository().delete(entity);
	}

	/**
	 * Used to refresh the entity for example for lazy fetching
	 * FETCH-Joins<br />
	 * This method is useful for example to refresh {@link SpecialLocation}
	 * galaxy
	 * 
	 * @param id
	 * @return The refreshed entity
	 * @author Kevin Guanche Darias
	 */
	public default E refresh(Number id) {
		return getRepository().findOne(id);
	}
}
