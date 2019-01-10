package com.kevinguanchedarias.sgtjava.business;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;
import com.kevinguanchedarias.sgtjava.entity.SpecialLocation;
import com.kevinguanchedarias.sgtjava.exception.ProgrammingException;
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

	/**
	 * Returns a reference, useful to execute uddate operations
	 * 
	 * @param id
	 * @return
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public default E getOne(Number id) {
		return getRepository().getOne(id);
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

	/**
	 * 
	 * @param id
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public default void delete(Number id) {
		delete(findByIdOrDie(id));
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

	public default EntityManager getEntityManager() {
		throw new ProgrammingException("Looks like this class needs the entity manager " + this.getClass().getName());
	}

	/**
	 * Checks if the passed entity is persisted or not, would else throw an
	 * exception
	 * 
	 * @param entity
	 * @throws ProgrammingException
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public default void checkPersisted(Object entity) {
		if (!getEntityManager().contains(entity)) {
			throw new ProgrammingException("Method requires a persisted entity, transient passed");
		}
	}
}
