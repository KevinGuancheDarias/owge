package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serial;

@SuppressWarnings("rawtypes")
public class FakeBaseBo implements BaseBo<Number, EntityWithId<Number>, DtoFromEntity<EntityWithId<Number>>> {
    public static final String CACHE_TAG = "entity";

    @Serial
    private static final long serialVersionUID = -4027397354402261825L;

    private final JpaRepository<EntityWithId<Number>, Number> repository;
    private final TaggableCacheManager taggableCacheManager;

    public FakeBaseBo(JpaRepository<EntityWithId<Number>, Number> repository, TaggableCacheManager taggableCacheManager) {
        this.repository = repository;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public JpaRepository<EntityWithId<Number>, Number> getRepository() {
        return repository;
    }

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return CACHE_TAG;
    }

    @Override
    public Class getDtoClass() {
        return null;
    }
}
