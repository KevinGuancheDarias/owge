package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Component
public class UnitListener {

    private final ImprovementBo improvementBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObjectRelationBo objectRelationBo;

    @Lazy
    public UnitListener(ImprovementBo improvementBo, ObtainedUnitBo obtainedUnitBo, ObtainedUnitRepository obtainedUnitRepository, ObjectRelationBo objectRelationBo) {
        this.improvementBo = improvementBo;
        this.obtainedUnitBo = obtainedUnitBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.objectRelationBo = objectRelationBo;
    }

    @PostUpdate
    @PostPersist
    public void onSaveClearCacheIfRequired(Unit unit) {
        improvementBo.clearCacheEntriesIfRequired(unit, obtainedUnitBo);
    }

    @PreRemove
    public void onDeleteClearCacheIfRequired(Unit unit) {
        objectRelationBo.delete(objectRelationBo.findOne(ObjectEnum.UNIT, unit.getId()));
        Set<UserStorage> affectedUsers = new HashSet<>();
        obtainedUnitRepository.findByUnit(unit).forEach(obtainedUnit -> affectedUsers.add(obtainedUnit.getUser()));
        obtainedUnitRepository.deleteByUnit(unit);
        improvementBo.clearCacheEntriesIfRequired(unit, obtainedUnitBo);
        affectedUsers.forEach(user -> {
            obtainedUnitBo.emitObtainedUnitChange(user.getId());
            if (unit.getImprovement() != null) {
                improvementBo.emitUserImprovement(user);
            }
        });
    }
}
