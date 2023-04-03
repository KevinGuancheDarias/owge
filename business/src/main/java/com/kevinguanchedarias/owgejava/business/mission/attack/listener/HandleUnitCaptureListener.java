package com.kevinguanchedarias.owgejava.business.mission.attack.listener;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef.AfterAttackEndListener;
import com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef.AfterUnitKilledCalculationListener;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.rule.UnitRuleFinderService;
import com.kevinguanchedarias.owgejava.business.rule.type.unit.UnitCaptureRuleTypeProviderBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.listener.UnitCaptureContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class HandleUnitCaptureListener
        implements AfterUnitKilledCalculationListener, AfterAttackEndListener {
    public static final String CAPTURE_UNIT_CONTEXT_NAME = "ATTACK_UNIT";

    private final UnitRuleFinderService unitRuleFinderService;
    private final RuleBo ruleBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionReportBo missionReportBo;

    @Override
    public void onAfterUnitKilledCalculation(AttackInformation information, AttackObtainedUnit attacker, AttackObtainedUnit victim, long killed) {
        unitRuleFinderService.findRule(
                        UnitCaptureRuleTypeProviderBo.PROVIDER_ID,
                        attacker.getObtainedUnit().getUnit(),
                        victim.getObtainedUnit().getUnit()
                )
                .filter(rule -> ruleBo.hasExtraArg(rule, 0) && ruleBo.hasExtraArg(rule, 1))
                .map(ruleBo::findExtraArgs)
                .filter(args -> (Math.random()) * 100 < Long.parseLong(args.get(0)))
                .map(args -> Long.parseLong(args.get(1)))
                .map(captureCountPercentage -> Math.floor((Math.random() * Math.floor(killed * (captureCountPercentage * 0.01))) + 1))
                .ifPresent(captured -> saveCaptured(information, attacker, victim, captured));
    }

    @Override
    public void onAttackEnd(AttackInformation information) {
        var contextData = information.getContextData(CAPTURE_UNIT_CONTEXT_NAME, UnitCaptureContext.class);
        var usersThatCaptured = contextData.stream()
                .map(unitCaptureContext -> unitCaptureContext.getCaptorUnit().getUser().getUser())
                .distinct()
                .toList();
        usersThatCaptured.forEach(userThatCaptured -> {
            var builder = UnitMissionReportBuilder.create(userThatCaptured, information.getAttackMission().getSourcePlanet(), information.getTargetPlanet(), List.of());
            builder.withUnitCaptureInformation(contextData.stream()
                    .filter(context -> context.getCaptorUnit().getUser().getUser().equals(userThatCaptured))
                    .toList()
            );
            missionReportBo.create(builder, false, userThatCaptured);
        });
    }

    private void saveCaptured(
            AttackInformation attackInformation,
            AttackObtainedUnit attacker,
            AttackObtainedUnit victim,
            Double captured
    ) {
        var attackerObtainedUnit = attacker.getObtainedUnit();
        var attackerMission = attackerObtainedUnit.getMission();
        var ou = new ObtainedUnit();
        var capturedCount = captured.longValue();
        var attackerUser = attackerObtainedUnit.getUser();
        ou.setUnit(victim.getObtainedUnit().getUnit());
        ou.setUser(attackerUser);
        ou.setCount(capturedCount);
        ou.setFromCapture(true);
        if (attackerMission != null) {
            ou.setSourcePlanet(attackerMission.getSourcePlanet());
            ou.setTargetPlanet(attackerMission.getTargetPlanet());
        } else {
            ou.setTargetPlanet(attackerObtainedUnit.getSourcePlanet());
        }
        addToContext(
                attackInformation,
                UnitCaptureContext.builder().captorUnit(attacker).victimUnit(victim).capturedUnits(capturedCount).build()
        );
        obtainedUnitBo.moveUnit(
                ou,
                ou.getUser().getId(),
                ou.getTargetPlanet().getId()
        );
    }

    private void addToContext(AttackInformation information, UnitCaptureContext unitCaptureContext) {
        information.addToContext(CAPTURE_UNIT_CONTEXT_NAME, unitCaptureContext);
    }
}
