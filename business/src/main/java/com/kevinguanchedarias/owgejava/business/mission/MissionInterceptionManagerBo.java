package com.kevinguanchedarias.owgejava.business.mission;


import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnitInterceptionFinderBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import com.kevinguanchedarias.owgejava.pojo.mission.MissionInterceptionInformation;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MissionInterceptionManagerBo {
    private final UnitInterceptionFinderBo unitInterceptionFinderBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final MissionUnitsFinderBo missionUnitsFinderBo;
    private final MissionReportManagerBo missionReportManagerBo;

    public MissionInterceptionInformation loadInformation(Mission mission, MissionType missionType) {
        boolean isMissionIntercepted = false;
        int totalInterceptedUnits;
        var missionId = mission.getId();
        var involvedUnits = missionUnitsFinderBo.findUnitsInvolved(missionId);
        List<ObtainedUnit> originallyInvolved = involvedUnits;
        List<InterceptedUnitsInformation> interceptedUnits;
        if (!missionType.equals(MissionType.RETURN_MISSION)) {
            interceptedUnits = unitInterceptionFinderBo.checkInterceptsSpeedImpactGroup(mission, involvedUnits);
            totalInterceptedUnits = interceptedUnits.stream().map(current -> current.getInterceptedUnits().size())
                    .reduce(Integer::sum).orElse(0);
            isMissionIntercepted = totalInterceptedUnits == involvedUnits.size();
            if (totalInterceptedUnits > 0) {
                deleteInterceptedUnits(interceptedUnits);
                involvedUnits = missionUnitsFinderBo.findUnitsInvolved(missionId);
            }
        } else {
            totalInterceptedUnits = 0;
            interceptedUnits = null;
        }
        return MissionInterceptionInformation.builder()
                .isMissionIntercepted(isMissionIntercepted)
                .totalInterceptedUnits(totalInterceptedUnits)
                .involvedUnits(involvedUnits)
                .originallyInvolved(originallyInvolved)
                .interceptedUnits(interceptedUnits)
                .build();
    }

    public void maybeAppendDataToMissionReport(
            Mission mission, UnitMissionReportBuilder reportBuilder, MissionInterceptionInformation interceptionInformation
    ) {
        if (interceptionInformation.getTotalInterceptedUnits() != 0 && reportBuilder != null) {
            reportBuilder.withInvolvedUnits(interceptionInformation.getOriginallyInvolved());
            List<InterceptedUnitsInformation> interceptedUnits = interceptionInformation.getInterceptedUnits();
            reportBuilder.withInterceptionInformation(interceptedUnits);
            unitInterceptionFinderBo.sendReportToInterceptorUsers(interceptedUnits, mission.getSourcePlanet(), mission.getTargetPlanet());
        }
    }

    public void handleMissionInterception(Mission mission, MissionInterceptionInformation missionInterceptionInformation) {
        mission.setResolved(true);
        List<InterceptedUnitsInformation> interceptedUnits = missionInterceptionInformation.getInterceptedUnits();
        reportFullMissionInterception(
                mission, missionInterceptionInformation.getOriginallyInvolved(), interceptedUnits
        );
        deleteInterceptedUnits(interceptedUnits);
        unitInterceptionFinderBo.sendReportToInterceptorUsers(interceptedUnits, mission.getSourcePlanet(), mission.getTargetPlanet());
    }

    private void deleteInterceptedUnits(List<InterceptedUnitsInformation> interceptedUnitsInformations) {
        interceptedUnitsInformations.stream().map(interception -> List.copyOf(interception.getInterceptedUnits())).forEach(obtainedUnitRepository::deleteAll);
    }

    private void reportFullMissionInterception(Mission mission, List<ObtainedUnit> involved, List<InterceptedUnitsInformation> interceptedUnits) {
        UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(mission.getUser(), mission.getSourcePlanet(),
                mission.getTargetPlanet(), involved).withInterceptionInformation(interceptedUnits);
        missionReportManagerBo.handleMissionReportSave(mission, builder);
    }
}
