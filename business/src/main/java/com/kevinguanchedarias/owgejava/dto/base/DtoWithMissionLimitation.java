package com.kevinguanchedarias.owgejava.dto.base;

import com.kevinguanchedarias.owgejava.entity.EntityWithMissionLimitation;
import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import lombok.Data;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
public class DtoWithMissionLimitation<E extends EntityWithMissionLimitation<?>> {
    private MissionSupportEnum canExplore = MissionSupportEnum.ANY;
    private MissionSupportEnum canGather = MissionSupportEnum.ANY;
    private MissionSupportEnum canEstablishBase = MissionSupportEnum.ANY;
    private MissionSupportEnum canAttack = MissionSupportEnum.ANY;
    private MissionSupportEnum canCounterattack = MissionSupportEnum.ANY;
    private MissionSupportEnum canConquest = MissionSupportEnum.ANY;
    private MissionSupportEnum canDeploy = MissionSupportEnum.ANY;

    protected void defineMissionLimitation(E source) {
        canExplore = source.getCanExplore();
        canGather = source.getCanGather();
        canEstablishBase = source.getCanEstablishBase();
        canAttack = source.getCanAttack();
        canCounterattack = source.getCanCounterattack();
        canConquest = source.getCanConquest();
        canDeploy = source.getCanDeploy();
    }
}
