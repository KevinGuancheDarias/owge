package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface BaseBo<K extends Serializable, E extends EntityWithId<K>, D extends DtoFromEntity<E>>
		extends Serializable, WithToDtoTrait<E, D>, BaseReadBo<K, E> {
	@Override
	JpaRepository<E, K> getRepository();

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
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default void delete(K id) {
		delete(findByIdOrDie(id));
	}

	default  void delete(List<E> entities) {
		getRepository().deleteAll(entities);
	}

	/**
	 * Used to refresh the entity for example for lazy fetching FETCH-Joins<br />
	 * This method is useful for example to refresh {@link SpecialLocation} galaxy
	 *
	 * @deprecated Use {@link BaseBo#refreshOptional(EntityWithId)} instead
	 * @param entity Notice: doesn't refresh the source entity
	 * @return The refreshed entity
	 * @author Kevin Guanche Darias
	 */
	@Deprecated(since = "0.9.19")
	default E refresh(E entity) {
		return getRepository().findById(entity.getId()).get();
	}

	/**
	 *
	 * @since 0.9.19
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	default Optional<E> refreshOptional(E entity) {
		return getRepository().findById(entity.getId());
	}

	default EntityManager getEntityManager() {
		throw new ProgrammingException("Looks like this class needs the entity manager " + getClass().getName());
	}

	/**
	 * Checks if the passed entity is persisted or not, would else throw an
	 * exception
	 *
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
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.0
	 */
	@Override
	default E onFind(E entity) {
		return entity;
	}
}
