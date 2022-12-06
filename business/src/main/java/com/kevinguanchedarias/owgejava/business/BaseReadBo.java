package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

public interface BaseReadBo<K extends Serializable, E extends EntityWithId<K>> {

    JpaRepository<E, K> getRepository();


    default List<E> findAll() {
        return getRepository().findAll().stream().map(this::onFind).toList();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.19
     */
    default List<E> findAll(Sort sort) {
        return getRepository().findAll(sort).stream().map(this::onFind).toList();
    }

    default long countAll() {
        return getRepository().count();
    }

    /**
     * Returns a reference, useful to execute update operations
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    default E getOne(K id) {
        return id == null
                ? null
                : getRepository().getById(id);
    }

    default E findById(K id) {
        return onFind(getRepository().findById(id).orElse(null));
    }

    default E findByIdOrDie(K id) {
        return onFind(SpringRepositoryUtil.findByIdOrDie(getRepository(), id));
    }

    /**
     * Can be used to alter all calls to findBy
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    default E onFind(E entity) {
        return entity;
    }

    default boolean exists(E entity) {
        return exists(entity.getId());
    }

    default boolean exists(K id) {
        return getRepository().existsById(id);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    default void existsOrDie(K id) {
        if (!exists(id)) {
            throwNotFound(id);
        }
    }

    private void throwNotFound(K id) {
        throw NotFoundException.fromAffected(SpringRepositoryUtil.findEntityClass(getRepository()), id);
    }
}
