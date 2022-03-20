package com.kevinguanchedarias.owgejava.business.mission.attack.listener;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef.AfterAttackEndListener;
import com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef.AfterUnitKilledCalculationListener;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.rule.itemtype.UnitRuleItemTypeProviderBo;
import com.kevinguanchedarias.owgejava.business.rule.type.UnitCaptureRuleTypeProviderBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.listener.UnitCaptureContext;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service
@AllArgsConstructor
public class HandleUnitCaptureListener
        implements AfterUnitKilledCalculationListener, AfterAttackEndListener {
    public static final String UNIT_TYPE = "UNIT_TYPE";
    public static final String CAPTURE_UNIT_CONTEXT_NAME = "ATTACK_UNIT";

    private final RuleRepository ruleRepository;
    private final RuleBo ruleBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionReportBo missionReportBo;

    @Override
    public void onAfterUnitKilledCalculation(AttackInformation information, AttackObtainedUnit attacker, AttackObtainedUnit victim, long killed) {
        var attackerUnitType = attacker.getObtainedUnit().getUnit().getType();
        var victimUnitType = victim.getObtainedUnit().getUnit().getType();
        unitVsUnitOptional(attacker, victim)
                .or(() -> unitVsUnitTypeOptional(attacker, victimUnitType))
                .or(() -> unitTypeVsUnitOptional(attackerUnitType, victim))
                .or(() -> unitTypeVsUnitTypeOptional(attackerUnitType, victimUnitType))
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

    private Optional<Rule> unitVsUnitOptional(AttackObtainedUnit attacker, AttackObtainedUnit victim) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                UnitCaptureRuleTypeProviderBo.PROVIDER_ID,
                UnitRuleItemTypeProviderBo.PROVIDER_ID,
                attacker.getObtainedUnit().getUnit().getId().longValue(),
                UnitRuleItemTypeProviderBo.PROVIDER_ID,
                victim.getObtainedUnit().getUnit().getId().longValue()
        );
    }

    private Optional<Rule> unitVsUnitTypeOptional(AttackObtainedUnit attacker, UnitType victimUnitType) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                UnitCaptureRuleTypeProviderBo.PROVIDER_ID,
                "UNIT",
                attacker.getObtainedUnit().getUnit().getId().longValue(),
                UNIT_TYPE,
                victimUnitType.getId().longValue()
        ).or(() ->
                ofNullable(victimUnitType.getParent())
                        .flatMap(victimParentType -> unitVsUnitTypeOptional(attacker, victimParentType))
        );

    }

    private Optional<Rule> unitTypeVsUnitOptional(UnitType attackerUnitType, AttackObtainedUnit victim) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                UnitCaptureRuleTypeProviderBo.PROVIDER_ID,
                UNIT_TYPE,
                attackerUnitType.getId().longValue(),
                "UNIT",
                victim.getObtainedUnit().getUnit().getId().longValue()
        ).or(() ->
                ofNullable(attackerUnitType.getParent())
                        .flatMap(attackerParent -> unitTypeVsUnitOptional(attackerParent, victim))
        );
    }

    private Optional<Rule> unitTypeVsUnitTypeOptional(UnitType attackerUnitType, UnitType victimUnitType) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                UnitCaptureRuleTypeProviderBo.PROVIDER_ID,
                UNIT_TYPE,
                attackerUnitType.getId().longValue(),
                UNIT_TYPE,
                victimUnitType.getId().longValue()
        ).or(() ->
                ofNullable(victimUnitType.getParent())
                        .flatMap(victimParent -> unitTypeVsUnitTypeOptional(attackerUnitType, victimParent))
        ).or(() ->
                ofNullable(attackerUnitType.getParent())
                        .flatMap(attackerParent -> unitTypeVsUnitTypeOptional(attackerParent, victimUnitType))
        );
    }
}
