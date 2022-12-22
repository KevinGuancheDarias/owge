package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendRequirementException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendTargetNotUnlocked;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ObjectRelationBo implements BaseBo<Integer, ObjectRelation, ObjectRelationDto> {
    public static final String OBJECT_RELATION_CACHE_TAG = "object_relation";

    @Serial
    private static final long serialVersionUID = -8660185836978327225L;

    private static final Logger LOG = Logger.getLogger(ObjectRelationBo.class);

    private final ObjectEntityBo objectEntityBo;

    private final UnlockedRelationRepository unlockedRelationRepository;

    private final ObjectRelationsRepository objectRelationsRepository;

    private final RequirementInformationRepository requirementInformationRepository;

    @Override
    public JpaRepository<ObjectRelation, Integer> getRepository() {
        return objectRelationsRepository;
    }

    public String getCacheTag() {
        return OBJECT_RELATION_CACHE_TAG;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<ObjectRelationDto> getDtoClass() {
        return ObjectRelationDto.class;
    }

    /**
     * Extracts the object target object entity from the relation
     *
     * @return An instance of: Upgrade.class , Unit.class depending on to what
     * connection is doing the relation
     * @author Kevin Guanche Darias
     */
    @SuppressWarnings("unchecked")
    public <E> E unboxObjectRelation(ObjectRelation relation) {
        JpaRepository<E, Number> repository = objectEntityBo.findRepository(relation.getObject());
        return repository.findById(relation.getReferenceId()).get();
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> unboxObjectRelation(List<ObjectRelation> relations) {
        List<E> retVal = new ArrayList<>();
        if (!CollectionUtils.isEmpty(relations)) {
            BaseBo<Integer, EntityWithId<Integer>, DtoFromEntity<EntityWithId<Integer>>> bo = objectEntityBo
                    .findBo(relations.get(0).getObject());
            relations.forEach(current -> {
                E entity = (E) bo.findByIdOrDie(current.getReferenceId());
                retVal.add(entity);
            });
        }
        return retVal;
    }

    /**
     * Finds by type and ref id
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public ObjectRelation findOne(ObjectEnum objectEnum, Integer referenceId) {
        return objectRelationsRepository.findOneByObjectCodeAndReferenceId(objectEnum.name(), referenceId);
    }

    public Optional<ObjectRelation> findOneOpt(ObjectEnum objectEnum, Integer referenceId) {
        return Optional.ofNullable(findOne(objectEnum, referenceId));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public ObjectRelation create(ObjectEnum target, Integer referenceId) {
        ObjectRelation objectRelation = new ObjectRelation();
        objectRelation.setObject(objectEntityBo.findByDescription(target));
        objectRelation.setReferenceId(referenceId);
        return objectRelationsRepository.save(objectRelation);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public ObjectRelation findObjectRelationOrCreate(ObjectEnum target, Integer referenceId) {
        ObjectRelation objectRelation = findOne(target, referenceId);
        if (objectRelation != null) {
            return objectRelation;
        } else {
            LOG.debug("No object relation of type " + target.name() + " with refId " + referenceId
                    + " exists in the target db, will create one");
            return create(target, referenceId);
        }

    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<ObjectRelation> findObjectRelationsHavingRequirementType(RequirementTypeEnum type) {
        return objectRelationsRepository.findByRequirementsRequirementCode(type.name());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<ObjectRelation> findByRequirementTypeAndSecondValue(RequirementTypeEnum type, long secondValue) {
        return objectRelationsRepository.findByRequirementsRequirementCodeAndRequirementsSecondValue(type.name(),
                secondValue);
    }

    /**
     * Finds by type, secondValue, and where thirdValue is greater or equal to x<br>
     * Example resultant SQL: WHERE type = '$type' AND secondValue = '$secondValue'
     * AND thirdValue >= '$thidValue'
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<ObjectRelation> findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(
            RequirementTypeEnum type, long secondValue, long thirdValue) {
        return objectRelationsRepository
                .findByRequirementsRequirementCodeAndRequirementsSecondValueAndRequirementsThirdValueGreaterThanEqual(
                        type.name(), secondValue, thirdValue);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void checkValid(ObjectRelation relation) {
        if (relation == null) {
            throw new SgtBackendRequirementException("No existe la relación");
        }

        ObjectEntity object = relation.getObject();
        objectEntityBo.checkValid(object);

        JpaRepository repository = objectEntityBo.findRepository(object);
        if (!repository.existsById(relation.getReferenceId())) {
            throw new SgtBackendRequirementException("No se encontró ninguna referencia con id "
                    + relation.getReferenceId() + " para el repositorio " + object.getRepository());
        }

    }

    /**
     * Checks if the relation is unlocked
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public void checkIsUnlocked(UserStorage user, ObjectRelation relation) {
        checkIsUnlocked(user.getId(), relation.getId());
    }

    /**
     * Checks if the relation is unlocked
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public void checkIsUnlocked(Integer userId, Integer relationId) {
        if (unlockedRelationRepository.findOneByUserIdAndRelationId(userId, relationId) == null) {
            throw new SgtBackendTargetNotUnlocked("The target object relation has not been unlocked");
        }
    }

    @Transactional
    public void delete(ObjectRelation objectRelation) {
        requirementInformationRepository.deleteByRelation(objectRelation);
        unlockedRelationRepository.deleteByRelation(objectRelation);
        objectRelationsRepository.delete(objectRelation);
    }

    @Transactional
    public void delete(Integer id) {
        delete(findById(id));
    }

}
