package com.kevinguanchedarias.owgejava.entity.listener;


import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCacheByUser;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

@Component
@Lazy
@AllArgsConstructor
public class EntityWithByUserCacheTagListener {
    private final TaggableCacheManager taggableCacheManager;

    @PostUpdate
    @PostPersist
    @PostRemove
    void postUpdate(EntityWithTaggableCacheByUser<?> entityWithTaggableCacheByUser) {
        taggableCacheManager.evictByCacheTag(entityWithTaggableCacheByUser.getByUserCacheTag(), entityWithTaggableCacheByUser.getUser().getId());
    }
}
