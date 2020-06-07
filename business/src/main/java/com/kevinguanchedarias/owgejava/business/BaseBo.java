package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;

public interface BaseBo<K extends Serializable, E extends EntityWithId<K>, D extends DtoFromEntity<E>>
		extends Serializable, WithToDtoTrait<E, D> {
	JpaRepository<E, K> getRepository();

	default List<E> findAll() {
		return getRepository().findAll().stream().map(this::onFind).collect(Collectors.toList());
	}

	default Long countAll() {
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
	default E getOne(K id) {
		return getRepository().getOne(id);
	}

	default E findById(K id) {
		return onFind(getRepository().findById(id).get());
	}

	default E findByIdOrDie(K id) {
		Optional<E> retVal = getRepository().findById(id);
		if (!retVal.isPresent()) {
			throwNotFound(id);
		}
		return onFind(retVal.get());
	}

	default E save(E entity) {
		return getRepository().save(entity);
	}

	default void save(List<E> entities) {
		getRepository().saveAll(entities);
	}

	default E saveAndFlush(E entity) {
		return getRepository().saveAndFlush(entity);
	}

	default void delete(E entity) {
		getRepository().delete(entity);
	}

	/**
	 *
	 * @param id
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default void delete(K id) {
		delete(findByIdOrDie(id));
	}

	default boolean exists(E entity) {
		return exists(entity.getId());
	}

	default boolean exists(K id) {
		return getRepository().existsById(id);
	}

	/**
	 *
	 * @param entity
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default void existsOrDie(E entity) {
		existsOrDie(entity.getId());
	}

	/**
	 *
	 * @param id
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default void existsOrDie(K id) {
		if (!exists(id)) {
			throwNotFound(id);
		}
	}

	/**
	 * Used to refresh the entity for example for lazy fetching FETCH-Joins<br />
	 * This method is useful for example to refresh {@link SpecialLocation} galaxy
	 *
	 * @param entity Notice: doesn't refresh the source entity
	 * @return The refreshed entity
	 * @author Kevin Guanche Darias
	 */
	default E refresh(E entity) {
		return getRepository().findById(entity.getId()).get();
	}

	default EntityManager getEntityManager() {
		throw new ProgrammingException("Looks like this class needs the entity manager " + getClass().getName());
	}

	/**
	 * Checks if the passed entity is persisted or not, would else throw an
	 * exception
	 *
	 * @param entity
	 * @throws ProgrammingException
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default void checkPersisted(Object entity) {
		if (!getEntityManager().contains(entity)) {
			throw new ProgrammingException("Method requires a persisted entity, transient passed");
		}
	}

	/**
	 * Can be used to alter all calls to findBy
	 *
	 * @param entity
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default E onFind(E entity) {
		return entity;
	}

	private void throwNotFound(K id) {
		throw NotFoundException.fromAffected(SpringRepositoryUtil.findEntityClass(getRepository()), id);
	}
}
