package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.business.user.UserEnergyServiceBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequirementsPojo {
    private Double requiredPrimary;
    private Double requiredSecondary;
    private Double requiredEnergy;
    private Double requiredTime;

    /**
     * Checks if the user mets the requirement
     *
     * @param user                <b>MUST BE</b> a fully loaded user
     * @param userEnergyServiceBo Used to get the user energy
     * @author Kevin Guanche Darias
     */
    public boolean canRun(UserStorage user, UserEnergyServiceBo userEnergyServiceBo) {
        return user.getPrimaryResource() >= requiredPrimary && user.getSecondaryResource() >= requiredSecondary
                && (requiredEnergy == null || requiredEnergy == 0D
                || userEnergyServiceBo.findAvailableEnergy(user) >= requiredEnergy);
    }
}
