package com.kevinguanchedarias.owgejava.business.cache;

import com.kevinguanchedarias.owgejava.entity.EntityWithCache;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class CacheableEntityCrudBo {
    private final TaggableCacheManager taggableCacheManager;

    private Map<String, JpaRepository<?, ?>> repositoryMap;

    @PostConstruct
    public void init() {
        System.out.println("foo");
    }

    public <E extends EntityWithCache<K>, K> E save(E entity, JpaRepository<E, K> repository) {
        var isModification = entity.getId() != null;
        var saved = repository.save(entity);
        String cacheTag = entity.getCacheTag();
        if (isModification) {
            doEvictEntryFromCache(cacheTag, entity.getId());
        }
        clearCacheTagList(cacheTag);
        return saved;
    }

    public <E extends EntityWithCache<K>, K> E saveAndFlush(E entity, JpaRepository<E, K> repository) {
        var isModification = entity.getId() != null;
        var saved = repository.saveAndFlush(entity);
        String cacheTag = saved.getCacheTag();
        if (isModification) {
            doEvictEntryFromCache(cacheTag, saved.getId());
        }
        clearCacheTagList(cacheTag);
        return saved;
    }

    public <E extends EntityWithCache<K>, K> void delete(E entity, JpaRepository<E, K> repository) {
        repository.delete(entity);
        clear(entity);
    }

    public <E extends EntityWithCache<K>, K> void delete(List<E> entities, JpaRepository<E, K> repository) {
        if (CollectionUtils.isNotEmpty(entities)) {
            var cacheTag = entities.get(0).getCacheTag();
            repository.deleteAll(entities);
            entities.forEach(entity -> doEvictEntryFromCache(cacheTag, entity.getId()));
            clearCacheTagList(cacheTag);
        }
    }

    public <E extends EntityWithCache<K>, K> void clear(E entity) {
        String cacheTag = entity.getCacheTag();
        doEvictEntryFromCache(cacheTag, entity.getId());
        clearCacheTagList(cacheTag);
    }

    private void clearCacheTagList(String cacheTag) {
        taggableCacheManager.evictByCacheTag(cacheTag);
    }

    private void doEvictEntryFromCache(String cacheTag, Object id) {
        taggableCacheManager.evictByCacheTag(cacheTag, id);
    }
}
