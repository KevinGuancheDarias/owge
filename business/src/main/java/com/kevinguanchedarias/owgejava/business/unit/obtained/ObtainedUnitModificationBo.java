package com.kevinguanchedarias.owgejava.business.unit.obtained;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ObtainedUnitModificationBo {

    private final ObtainedUnitRepository repository;
    private final ImprovementBo improvementBo;
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;

    /**
     * Deletes obtained units involved in passed mission <br>
     * <b>NOTICE: </b> By default will subtract improvements
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByMissionId(Long missionId) {
        repository.deleteByMissionId(missionId);
        improvementBo.clearCacheEntries(obtainedUnitImprovementCalculationService);
    }
}
