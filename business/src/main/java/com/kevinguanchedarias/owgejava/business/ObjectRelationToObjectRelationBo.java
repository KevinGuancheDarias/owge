package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObjectRelationToObjectRelation;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
public class ObjectRelationToObjectRelationBo
        implements BaseBo<Integer, ObjectRelationToObjectRelation, DtoFromEntity<ObjectRelationToObjectRelation>> {
    public static final String OBJECT_RELATION_TO_OBJECT_RELATION_CACHE_TAG = "obj_rel_2_obj_rel";

    @Serial
    private static final long serialVersionUID = 9174432635190297289L;

    @Autowired
    @Lazy
    private transient ObjectRelationToObjectRelationRepository repository;

    @Autowired
    private transient TaggableCacheManager taggableCacheManager;

    @Override
    public Class<DtoFromEntity<ObjectRelationToObjectRelation>> getDtoClass() {
        throw new SgtBackendNotImplementedException("No DTO for now");
    }

    @Override
    public JpaRepository<ObjectRelationToObjectRelation, Integer> getRepository() {
        return repository;
    }

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return OBJECT_RELATION_TO_OBJECT_RELATION_CACHE_TAG;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<ObjectRelationToObjectRelation> findByMasterId(Integer relationId) {
        return repository.findByMasterId(relationId);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public ObjectRelationToObjectRelation findBySlave(ObjectRelation currentRelation) {
        return repository.findBySlaveId(currentRelation.getId());
    }

    /**
     * If the input relation is master (known because it's not a requirement group,
     * and has requirement groups
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public boolean isMaster(ObjectRelation relation) {
        return repository.existsByMaster(relation);
    }
}
