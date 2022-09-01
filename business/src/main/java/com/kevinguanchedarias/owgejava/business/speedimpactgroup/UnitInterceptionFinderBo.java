package com.kevinguanchedarias.owgejava.business.speedimpactgroup;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.AllianceBo;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@AllArgsConstructor
public class UnitInterceptionFinderBo {
    private final ObtainedUnitBo obtainedUnitBo;
    private final SpeedImpactGroupBo speedImpactGroupBo;
    private final AllianceBo allianceBo;
    private final MissionReportBo missionReportBo;


    public List<InterceptedUnitsInformation> checkInterceptsSpeedImpactGroup(Mission mission,
                                                                             List<ObtainedUnit> involvedUnits) {
        Set<ObtainedUnit> alreadyIntercepted = new HashSet<>();
        Map<Integer, InterceptedUnitsInformation> interceptedMap = new HashMap<>();
        List<ObtainedUnit> unitsWithInterception = obtainedUnitBo.findInvolvedInAttack(mission.getTargetPlanet())
                .stream().filter(current -> !CollectionUtils.isEmpty(current.getUnit().getInterceptableSpeedGroups()))
                .toList();
        unitsWithInterception.forEach(unitWithInterception -> involvedUnits.stream().filter(
                        involved -> speedImpactGroupBo.canIntercept(
                                unitWithInterception.getUnit().getInterceptableSpeedGroups(), involved.getUser(), involved.getUnit()
                        )
                ).filter(involved -> !alreadyIntercepted.contains(involved) && allianceBo.areEnemies(unitWithInterception.getUser(), involved.getUser()))
                .forEach(interceptedUnit -> {
                    UserStorage interceptorUser = unitWithInterception.getUser();
                    Integer interceptorUserId = interceptorUser.getId();
                    if (!interceptedMap.containsKey(interceptorUserId)) {
                        interceptedMap.put(interceptorUserId, new InterceptedUnitsInformation(
                                unitWithInterception.getUser(), unitWithInterception, new HashSet<>()));
                    }
                    interceptedMap.get(interceptorUserId).getInterceptedUnits().add(interceptedUnit);
                    alreadyIntercepted.add(interceptedUnit);
                }));
        return new ArrayList<>(interceptedMap.values());
    }

    public void sendReportToInterceptorUsers(List<InterceptedUnitsInformation> interceptedUnitsInformationList, Planet sourcePlanet, Planet targetPlanet) {
        interceptedUnitsInformationList.forEach(info -> this.doSendReportToInterceptorUser(info, sourcePlanet, targetPlanet));
    }

    private void doSendReportToInterceptorUser(InterceptedUnitsInformation interceptedUnitsInformation, Planet sourcePlanet, Planet targetPlanet) {
        UnitMissionReportBuilder unitMissionReportBuilder = UnitMissionReportBuilder.create(
                interceptedUnitsInformation.getInterceptorUser(),
                sourcePlanet,
                targetPlanet,
                List.of(interceptedUnitsInformation.getInterceptorUnit())
        );
        unitMissionReportBuilder.withInterceptionInformation(List.of(interceptedUnitsInformation));
        missionReportBo.create(unitMissionReportBuilder, true, interceptedUnitsInformation.getInterceptorUser());
    }
}
