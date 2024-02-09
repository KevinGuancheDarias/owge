package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitImprovementCalculationService;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Component
public class UnitListener {

    private final ImprovementBo improvementBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObjectRelationBo objectRelationBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;

    @Lazy
    public UnitListener(
            ImprovementBo improvementBo,
            ObtainedUnitRepository obtainedUnitRepository,
            ObjectRelationBo objectRelationBo,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService
    ) {
        this.improvementBo = improvementBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.objectRelationBo = objectRelationBo;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.obtainedUnitImprovementCalculationService = obtainedUnitImprovementCalculationService;
    }

    @PostUpdate
    @PostPersist
    public void onSaveClearCacheIfRequired(Unit unit) {
        improvementBo.clearCacheEntriesIfRequired(unit, obtainedUnitImprovementCalculationService);
    }

    @PreRemove
    public void onDeleteClearCacheIfRequired(Unit unit) {
        objectRelationBo.findOneOpt(ObjectEnum.UNIT, unit.getId()).ifPresent(objectRelationBo::delete);
        Set<UserStorage> affectedUsers = new HashSet<>();
        obtainedUnitRepository.findByUnit(unit).forEach(obtainedUnit -> affectedUsers.add(obtainedUnit.getUser()));
        obtainedUnitRepository.deleteByUnit(unit);
        improvementBo.clearCacheEntriesIfRequired(unit, obtainedUnitImprovementCalculationService);
        affectedUsers.forEach(user -> {
            obtainedUnitEventEmitter.emitObtainedUnits(user);
            if (unit.getImprovement() != null) {
                improvementBo.emitUserImprovement(user);
            }
        });
    }
}
