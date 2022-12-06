package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserEnergyServiceBo {
    private final ImprovementBo improvementBo;
    private final ObtainedUnitRepository obtainedUnitRepository;

    public Double findConsumedEnergy(UserStorage user) {
        return ObjectUtils.firstNonNull(obtainedUnitRepository.computeConsumedEnergyByUser(user), 0D);
    }

    public Double findMaxEnergy(UserStorage user) {
        var groupedImprovement = improvementBo.findUserImprovement(user);
        var faction = user.getFaction();
        return improvementBo.computeImprovementValue(faction.getInitialEnergy().floatValue(),
                groupedImprovement.getMoreEnergyProduction());
    }

    /**
     * Returns the available energy of the user <br>
     * <b>NOTICE: Expensive method </b>
     *
     * @todo For god's sake create a cache system
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Double findAvailableEnergy(UserStorage user) {
        return findMaxEnergy(user) - findConsumedEnergy(user);
    }
}
