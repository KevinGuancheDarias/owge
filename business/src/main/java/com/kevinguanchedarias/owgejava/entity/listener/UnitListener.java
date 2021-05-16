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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
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

    /**
     * If the unit doesn't have a speedImpactGroup will use the unitType one
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostLoad
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadSpeedImpactImprovement(Unit unit) {
        if (unit.getSpeedImpactGroup() == null) {
            unit.setSpeedImpactGroup(unit.getType().getSpeedImpactGroup());
        }
    }

    /**
     * If the unit speedImpact group matches parent, will save as null
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PreUpdate
    @PrePersist
    public void putNullIfMatchsUnitType(Unit unit) {
        var unitSpeedImpactGroup = unit.getSpeedImpactGroup();
        var unitType = unit.getType();
        if (unitSpeedImpactGroup != null && unitType != null && unitType.getSpeedImpactGroup() != null
                && unitType.getSpeedImpactGroup().getId().equals(unitSpeedImpactGroup.getId())) {
            unit.setSpeedImpactGroup(null);
        }
    }

    @PostUpdate
    @PostPersist
    public void onSaveClearCacheIfRequired(Unit unit) {
        improvementBo.clearCacheEntriesIfRequired(unit, obtainedUnitBo);
    }

    @PreRemove
    public void onDeleteClearCacheIfRequired(Unit unit) {
        objectRelationBo.delete(objectRelationBo.findOneByObjectTypeAndReferenceId(ObjectEnum.UNIT, unit.getId()));
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
