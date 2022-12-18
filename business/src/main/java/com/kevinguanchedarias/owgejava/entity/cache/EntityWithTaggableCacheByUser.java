package com.kevinguanchedarias.owgejava.entity.cache;

import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface EntityWithTaggableCacheByUser<K> extends EntityWithId<K> {
    String getByUserCacheTag();

    UserStorage getUser();
}
