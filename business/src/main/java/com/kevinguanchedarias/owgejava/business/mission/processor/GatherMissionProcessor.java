package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class GatherMissionProcessor implements MissionProcessor {
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final ImprovementBo improvementBo;
    private final SocketIoService socketIoService;
    private final TransactionUtilService transactionUtilService;
    private final AttackMissionProcessor attackMissionProcessor;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.GATHER;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        UserStorage user = mission.getUser();
        var faction = user.getFaction();
        var targetPlanet = mission.getTargetPlanet();
        boolean continueMission = attackMissionProcessor.triggerAttackIfRequired(mission, user, targetPlanet);
        if (continueMission) {
            returnMissionRegistrationBo.registerReturnMission(mission, null);
            Long gathered = involvedUnits.stream()
                    .map(current -> ObjectUtils.firstNonNull(current.getUnit().getCharge(), 0) * current.getCount())
                    .reduce(0L, Long::sum);
            double withPlanetRichness = gathered * targetPlanet.findRationalRichness();
            var groupedImprovement = improvementBo.findUserImprovement(user);
            double withUserImprovement = withPlanetRichness
                    + (withPlanetRichness * improvementBo.findAsRational(groupedImprovement.getMoreChargeCapacity()));
            var customPrimary = faction.getCustomPrimaryGatherPercentage();
            var customSecondary = faction.getCustomSecondaryGatherPercentage();
            double primaryResource;
            double secondaryResource;
            if (customPrimary != null && customSecondary != null && customPrimary > 0 && customSecondary > 0) {
                primaryResource = withUserImprovement * (customPrimary / 100);
                secondaryResource = withUserImprovement * (customSecondary / 100);
            } else {
                primaryResource = withUserImprovement * 0.5;
                secondaryResource = withUserImprovement * 0.5;
            }
            user.addtoPrimary(primaryResource);
            user.addToSecondary(secondaryResource);
            UnitMissionReportBuilder builder = UnitMissionReportBuilder
                    .create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
                    .withGatherInformation(primaryResource, secondaryResource);
            transactionUtilService.doAfterCommit(() -> socketIoService.sendMessage(user, "mission_gather_result", () -> {
                Map<String, Double> content = new HashMap<>();
                content.put("primaryResource", primaryResource);
                content.put("secondaryResource", secondaryResource);
                return content;
            }));
            mission.setResolved(true);
            return builder;
        } else {
            return null;
        }
    }
}
