package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface BaseReadBo<K extends Serializable,  E extends EntityWithId<K>> {

    JpaRepository<E, K> getRepository();


    default List<E> findAll() {
        return getRepository().findAll().stream().map(this::onFind).collect(Collectors.toList());
    }

    /**
     *
     * @since 0.9.19
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    default List<E> findAll(Sort sort) {
        return getRepository().findAll(sort).stream().map(this::onFind).collect(Collectors.toList());
    }

    default long countAll() {
        return getRepository().count();
    }

    /**
     * Returns a reference, useful to execute update operations
     *
     * @since 0.7.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    default E getOne(K id) {
        return id == null
                ? null
                : getRepository().getOne(id);
    }

    default E findById(K id) {
        return onFind(getRepository().findById(id).orElse(null));
    }

    default E findByIdOrDie(K id) {
        Optional<E> retVal = getRepository().findById(id);
        if (retVal.isEmpty()) {
            throwNotFound(id);
        }
        return onFind(retVal.get());
    }

    /**
     * Can be used to alter all calls to findBy
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
