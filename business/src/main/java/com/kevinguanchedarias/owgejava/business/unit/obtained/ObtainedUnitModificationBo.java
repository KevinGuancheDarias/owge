package com.kevinguanchedarias.owgejava.business.unit.obtained;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * Detaches the given missions from all obtained units that reference them via
     * {@code mission_id}, setting both the mission and target-planet columns to null.
     * <p>
     * Must be called <b>before</b> deleting the mission rows so that in-memory entities
     * and the improvement cache remain consistent.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.11.12
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void detachMissions(List<Mission> missions) {
        if (missions.isEmpty()) {
            return;
        }
        repository.detachMissions(missions);
        improvementBo.clearCacheEntries(obtainedUnitImprovementCalculationService);
    }
}
