package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface BaseBo<K extends Serializable, E extends EntityWithId<K>, D extends DtoFromEntity<E>>
        extends Serializable, WithToDtoTrait<E, D>, BaseReadBo<K, E> {
    @Override
    JpaRepository<E, K> getRepository();

    TaggableCacheManager getTaggableCacheManager();

    String getCacheTag();

    default E save(E entity) {
        var isModification = entity.getId() != null;
        var saved = getRepository().save(entity);
        if (isModification) {
            doEvictEntryFromCache(entity.getId());
        }
        clearCacheTagList();
        return saved;
    }

    default void save(List<E> entities) {
        var modifications = entities.stream()
                .filter(entity -> entity.getId() != null)
                .map(EntityWithId::getId);
        getRepository().saveAll(entities);
        modifications.forEach(
                modificationId -> getTaggableCacheManager().evictByCacheTag(getCacheTag(), modificationId)
        );
        clearCacheTagList();
    }

    default E saveAndFlush(E entity) {
        var isModification = entity.getId() != null;
        var saved = getRepository().saveAndFlush(entity);
        if (isModification) {
            doEvictEntryFromCache(saved.getId());
        }
        clearCacheTagList();
        return saved;
    }

    default void delete(E entity) {
        getRepository().delete(entity);
        doEvictEntryFromCache(entity.getId());
        clearCacheTagList();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    default void delete(K id) {
        delete(findByIdOrDie(id));
    }

    default void delete(List<E> entities) {
        getRepository().deleteAll(entities);
        entities.forEach(entity -> doEvictEntryFromCache(entity.getId()));
        clearCacheTagList();
    }

    /**
     * Used to refresh the entity for example for lazy fetching FETCH-Joins<br />
     * This method is useful for example to refresh {@link SpecialLocation} galaxy
     *
     * @param entity Notice: doesn't refresh the source entity
     * @return The refreshed entity
     * @author Kevin Guanche Darias
     * @deprecated Use {@link BaseBo#refreshOptional(EntityWithId)} instead
     */
    @Deprecated(since = "0.9.19")
    default E refresh(E entity) {
        return getRepository().findById(entity.getId()).get();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.19
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

    default void clearCacheTagList() {
        getTaggableCacheManager().evictByCacheTag(getCacheTag());
    }

    default void doEvictEntryFromCache(K id) {
        getTaggableCacheManager().evictByCacheTag(getCacheTag(), id);
    }
}
