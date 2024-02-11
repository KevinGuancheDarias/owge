package com.kevinguanchedarias.owgejava.entity.listener;


import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCacheByUser;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

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
