package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

@Service
@Transactional
public class UnlockedRelationBo implements BaseBo<Long, UnlockedRelation, DtoFromEntity<UnlockedRelation>> {
    public static final String UNLOCKED_RELATION_CACHE_TAG = "unlocked_relation";

    @Serial
    private static final long serialVersionUID = 8586133814355378376L;

    @Autowired
    private UnlockedRelationRepository repository;

    @Autowired
    private ObjectRelationBo objectRelationBo;

    public UnlockedRelation findOneByUserIdAndRelationId(Integer userId, Integer relationId) {
        return repository.findOneByUserIdAndRelationId(userId, relationId);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<UnlockedRelation> findByUserIdAndObjectType(Integer userId, ObjectEnum type) {
        return repository.findByUserIdAndRelationObjectCode(userId, type.name());
    }

    /**
     * Unbox to target entity, for example will return a list of Units
     *
     * @return List of Object Entities
     * @author Kevin Guanche Darias
     */
    public <E> List<E> unboxToTargetEntity(List<UnlockedRelation> unlockedRelations) {
        return objectRelationBo.unboxObjectRelation(unboxUnlockedRelationList(unlockedRelations));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void deleteByRelation(ObjectRelation objectRelation) {
        repository.deleteByRelation(objectRelation);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public boolean isUnlocked(UserStorage user, ObjectRelation relation) {
        return repository.existsByUserAndRelation(user, relation);
    }

    @Override
    public JpaRepository<UnlockedRelation, Long> getRepository() {
        return repository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<DtoFromEntity<UnlockedRelation>> getDtoClass() {
        throw new SgtBackendNotImplementedException("UnlockedRelation doesn't have a dto ... for now =/");
    }

    /**
     * Converts a list of unlocked relations into a list of relations
     *
     * @param unlockedRelations list of unlocked relations
     * @return list of relations
     * @author Kevin Guanche Darias
     */
    private List<ObjectRelation> unboxUnlockedRelationList(List<UnlockedRelation> unlockedRelations) {
        return unlockedRelations.stream().map(UnlockedRelation::getRelation).toList();
    }

}
