package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCache;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

@Component
@Lazy
@AllArgsConstructor
public class EntityWithTaggableCacheListener {
    private final TaggableCacheManager taggableCacheManager;

    @PostUpdate
    public void postUpdate(EntityWithTaggableCache<Object> entity) {
        doEvictExisting(entity);
    }

    @PostPersist
    public void postPersist(EntityWithTaggableCache<Object> entity) {
        taggableCacheManager.evictByCacheTag(entity.getCacheTag());
    }

    @PostRemove
    public void postRemove(EntityWithTaggableCache<Object> entity) {
        doEvictExisting(entity);
    }

    private void doEvictExisting(EntityWithTaggableCache<Object> entity) {
        var cacheTag = entity.getCacheTag();
        taggableCacheManager.evictByCacheTag(cacheTag);
        taggableCacheManager.evictByCacheTag(cacheTag, entity.getId());
    }
}
