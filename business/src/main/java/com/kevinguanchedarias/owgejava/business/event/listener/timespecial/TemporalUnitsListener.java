package com.kevinguanchedarias.owgejava.business.event.listener.timespecial;

import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.ScheduledTasksManagerService;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.schedule.TemporalUnitScheduleListener;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.jdbc.ObtainedUnitTemporalInformation;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveTemporalUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID;

@Service
@AllArgsConstructor
@Slf4j
public class TemporalUnitsListener {
    private final ObtainedUnitBo obtainedUnitBo;
    private final RuleBo ruleBo;
    private final UnitRepository unitRepository;
    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;
    private final ScheduledTasksManagerService scheduledTasksManagerService;

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onTimeSpecialActivated(ActiveTimeSpecial activeTimeSpecial) {
        handleRules(ruleBo.findByOriginTypeAndOriginIdAndType(
                ObjectEnum.TIME_SPECIAL.name(),
                activeTimeSpecial.getTimeSpecial().getId(),
                TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID
        ), activeTimeSpecial.getUser());
    }

    private void handleRules(List<RuleDto> rules, UserStorage user) {
        var map = new HashMap<Long, Set<ObtainedUnit>>();

        rules.stream()
                .filter(ruleDto -> ruleDto.getExtraArgs().size() == 2 && ObjectEnum.UNIT.name().equals(ruleDto.getDestinationType()))
                .map(ruleDto -> {
                    var opt = unitRepository.findById(Math.toIntExact(ruleDto.getDestinationId()));
                    if (opt.isEmpty()) {
                        log.warn("Unit with id {} doesn't exists for rule {}", ruleDto.getDestinationId(), ruleDto.getId());
                    }
                    return Pair.of(opt, ruleDto);
                })
                .filter(pair -> pair.getLeft().isPresent())
                .map(pair -> Pair.of(pair.getLeft().get(), pair.getRight()))
                .forEach(pair -> resolveUnit(pair.getLeft(), pair.getRight(), user, map));
        var isChanged = new AtomicBoolean(false);
        map.forEach((key, ouList) -> {
            var temporalInformation = obtainedUnitTemporalInformationRepository.save(
                    ObtainedUnitTemporalInformation.builder()
                            .duration(key)
                            .expiration(Instant.now().plusSeconds(key))
                            .build()
            );

            ouList.forEach(ou -> ou.setExpirationId(temporalInformation.getId()));
            scheduleTask(temporalInformation);
            obtainedUnitBo.save(ouList.stream().toList());
            isChanged.set(true);
        });
        if (isChanged.get()) {
            obtainedUnitBo.emitObtainedUnitChange(user.getId());
        }
    }

    private void scheduleTask(ObtainedUnitTemporalInformation temporalInformation) {
        var task = new ScheduledTask(TemporalUnitScheduleListener.TASK_NAME, temporalInformation.getId());
        scheduledTasksManagerService.registerEvent(task, temporalInformation.getDuration());
    }

    private void resolveUnit(Unit unit, RuleDto ruleDto, UserStorage user, Map<Long, Set<ObtainedUnit>> mutableMap) {
        var extraArgs = ruleDto.getExtraArgs();
        var duration = Long.parseLong((String) extraArgs.get(0));
        var count = Long.parseLong((String) extraArgs.get(1));
        var ou = ObtainedUnit.builder()
                .unit(unit)
                .user(user)
                .sourcePlanet(user.getHomePlanet())
                .count(count)
                .build();
        if (mutableMap.containsKey(duration)) {
            mutableMap.get(duration).add(ou);
        } else {
            mutableMap.put(duration, new HashSet<>(List.of(ou)));
        }
    }

}
