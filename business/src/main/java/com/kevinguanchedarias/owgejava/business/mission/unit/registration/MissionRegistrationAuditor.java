package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.business.audit.AuditBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MissionRegistrationAuditor {
    private final AuditBo auditBo;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;

    @Transactional(propagation = Propagation.MANDATORY)
    public void auditMissionRegistration(Mission mission, boolean isDeploy) {
        var planetOwner = mission.getTargetPlanet().getOwner();
        if (planetOwner == null || planetOwner.getId().equals(mission.getUser().getId())) {
            auditBo.doAudit(AuditActionEnum.REGISTER_MISSION, mission.getType().getCode(), null);
        } else {
            auditBo.doAudit(AuditActionEnum.REGISTER_MISSION, mission.getType().getCode(), planetOwner.getId());
        }
        if (isDeploy) {
            obtainedUnitFinderBo.findInPlanetOrInMissionToPlanet(mission.getTargetPlanet()).stream()
                    .map(ObtainedUnit::getUser)
                    .filter(user -> !user.getId().equals(mission.getUser().getId()))
                    .distinct()
                    .forEach(unitUser -> auditBo.doAudit(AuditActionEnum.USER_INTERACTION, "DEPLOY", unitUser.getId()));
        }
    }
}
