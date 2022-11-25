package com.kevinguanchedarias.owgejava.business.cache;


import com.kevinguanchedarias.owgejava.entity.EntityWithCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public abstract class AbstractCacheableEntityBo<E extends EntityWithCache<K>, K> {
    public abstract JpaRepository<E, K> getRepository();

    @Autowired
    private CacheableEntityCrudBo cacheableEntityCrudBo;

    protected E save(E entity) {
        return cacheableEntityCrudBo.save(entity, getRepository());
    }

    protected void delete(E entity) {
        cacheableEntityCrudBo.delete(entity, getRepository());
    }

    protected void delete(List<E> entities) {
        cacheableEntityCrudBo.delete(entities, getRepository());
    }
}
