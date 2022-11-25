package com.kevinguanchedarias.owgejava.entity;

public interface EntityWithCache<K> extends EntityWithId<K> {
    String getCacheTag();
}
