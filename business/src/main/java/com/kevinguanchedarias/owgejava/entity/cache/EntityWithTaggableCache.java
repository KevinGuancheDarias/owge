package com.kevinguanchedarias.owgejava.entity.cache;

import com.kevinguanchedarias.owgejava.entity.EntityWithId;

public interface EntityWithTaggableCache<K> extends EntityWithId<K> {
    String getCacheTag();
}
