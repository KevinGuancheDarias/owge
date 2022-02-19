package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.AbstractMissionBo;
import com.kevinguanchedarias.owgejava.business.AllianceBo;
import com.kevinguanchedarias.owgejava.business.AttackRuleBo;
import com.kevinguanchedarias.owgejava.business.CriticalAttackBo;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.OwgeElementSideDeletedException;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnitWithScore;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
@Slf4j
public class AttackMissionManagerBo {

    private final ObtainedUnitBo obtainedUnitBo;
    private final ImprovementBo improvementBo;
    private final AttackObtainedUnitBo attackObtainedUnitBo;
    private final AttackRuleBo attackRuleBo;
    private final CriticalAttackBo criticalAttackBo;
    private final MissionRepository missionRepository;
    private final UserStorageBo userStorageBo;
    private final UnitTypeBo unitTypeBo;
    private final SocketIoService socketIoService;
    private final MissionBo missionBo;
    private final AllianceBo allianceBo;
    private final AttackEventEmitter attackEventEmitter;

    public AttackInformation buildAttackInformation(Planet targetPlanet, Mission attackMission) {
        AttackInformation retVal = new AttackInformation(attackMission, targetPlanet);
        obtainedUnitBo.findInvolvedInAttack(targetPlanet).forEach(unit -> {
            if (!attackMission.equals(unit.getMission())) {
                addUnit(retVal, unit);
            }
        });
        obtainedUnitBo.findByMissionId(attackMission.getId()).forEach(unit -> addUnit(retVal, unit));
        return retVal;
    }

    public void addUnit(AttackInformation attackInformation, ObtainedUnit unitEntity) {
        UserStorage userEntity = unitEntity.getUser();
        AttackUserInformation user;
        var users = attackInformation.getUsers();
        if (users.containsKey(userEntity.getId())) {
            user = users.get(userEntity.getId());
        } else {
            user = new AttackUserInformation(userEntity, improvementBo.findUserImprovement(userEntity));
            users.put(userEntity.getId(), user);
        }
        var unit = attackObtainedUnitBo.create(unitEntity, user);
        user.getUnits().add(unit);
        attackInformation.getUnits().add(unit);
    }

    public void startAttack(AttackInformation attackInformation) {
        var units = attackInformation.getUnits();
        var users = attackInformation.getUsers();
        attackObtainedUnitBo.shuffleUnits(units);
        users.forEach((userId, user) -> user.setAttackableUnits(units.stream().filter(
                unit -> !unit.getUser().getUser().getId().equals(user.getUser().getId())
                        && allianceBo.areEnemies(user.getUser(), unit.getUser().getUser())
        ).toList()));
        doAttack(attackInformation);
        updatePoints(attackInformation);
        attackInformation.getUsersWithDeletedMissions().forEach(userId -> {
            missionBo.emitUnitMissions(userId);
            userStorageBo.emitUserData(userStorageBo.findById(userId));
            attackInformation.getUsersWithChangedCounts().remove(userId);
        });
        var targetPlanet = attackInformation.getTargetPlanet();
        attackInformation.getUsersWithChangedCounts().forEach(userId -> {
            if (targetPlanet.getOwner() != null && targetPlanet.getOwner().getId().equals(userId)) {
                obtainedUnitBo.emitObtainedUnitChange(userId);
                if (!attackInformation.getUsersWithDeletedMissions().isEmpty()
                        || attackInformation.getUsersWithChangedCounts().size() > 1) {
                    missionBo.emitEnemyMissionsChange(targetPlanet.getOwner());
                }
            }
            missionBo.emitUnitMissions(userId);
            userStorageBo.emitUserData(userStorageBo.findById(userId));
        });
        attackEventEmitter.emitAttackEnd(attackInformation);
    }

    private void updatePoints(AttackInformation attackInformation) {
        Set<Integer> alteredUsers = new HashSet<>();
        attackInformation.getUsers().entrySet().forEach(current -> {
            var attackUserInformation = current.getValue();
            List<AttackObtainedUnit> userUnits = attackUserInformation.getUnits();
            userStorageBo.addPointsToUser(attackUserInformation.getUser(), attackUserInformation.getEarnedPoints());
            userUnits.stream().filter(currentUnit -> !currentUnit.getFinalCount().equals(0L)
                    && !currentUnit.getInitialCount().equals(currentUnit.getFinalCount())).forEach(currentUnit -> {
                long killed = currentUnit.getInitialCount() - currentUnit.getFinalCount();
                try {
                    obtainedUnitBo.trySave(currentUnit.getObtainedUnit(), -killed);
                    alteredUsers.add(attackUserInformation.getUser().getId());
                } catch (OwgeElementSideDeletedException e) {
                    log.warn("Element side deleted", e);
                }
            });
        });
        alteredUsers.addAll(attackInformation.getUsersWithChangedCounts());
        TransactionUtil.doAfterCommit(() -> alteredUsers.forEach(current -> {
            unitTypeBo.emitUserChange(current);
            socketIoService.sendMessage(current, AbstractMissionBo.UNIT_OBTAINED_CHANGE,
                    () -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(current)));

        }));
    }

    private void doAttack(AttackInformation attackInformation) {
        attackInformation.getUnits().forEach(attackerUnit -> {
            List<AttackObtainedUnitWithScore> attackableByUnit = attackerUnit.getUser().getAttackableUnits().stream()
                    .filter(target -> {
                        var unitEntity = attackerUnit.getObtainedUnit().getUnit();
                        var attackRule = ObjectUtils.firstNonNull(unitEntity.getAttackRule(),
                                attackRuleBo.findAttackRule(unitEntity.getType()));
                        return attackRuleBo.canAttack(attackRule, target.getObtainedUnit());
                    })
                    .map(target -> new AttackObtainedUnitWithScore(target, findCriticalScore(attackerUnit, target)))
                    .sorted((a, b) -> (int) (b.getScore() - a.getScore()))
                    .toList();
            for (AttackObtainedUnitWithScore target : attackableByUnit) {
                if (!target.getAttackObtainedUnit().getFinalCount().equals(0L)) {
                    attackTarget(attackInformation, attackerUnit, target);
                }
                if (attackerUnit.isNoAttack()) {
                    break;
                }
            }
        });
    }

    private void attackTarget(AttackInformation attackInformation, AttackObtainedUnit source, AttackObtainedUnitWithScore targetWithScore) {
        var originalAttackValue = source.getPendingAttack();
        var myAttack = source.getPendingAttack() * targetWithScore.getScore();
        boolean bypassShield = source.getObtainedUnit().getUnit().getBypassShield();
        var target = targetWithScore.getAttackObtainedUnit();
        var victimHealth = bypassShield ? target.getAvailableHealth()
                : target.getAvailableHealth() + target.getAvailableShield();
        addPointsAndUpdateCount(attackInformation, myAttack, source, target);
        if (victimHealth > myAttack) {
            source.setPendingAttack(0D);
            source.setNoAttack(true);
            if (bypassShield) {
                target.setAvailableHealth(target.getAvailableHealth() - myAttack);
            } else {
                double attackDistributed = myAttack / 2;
                target.setAvailableShield(target.getAvailableShield() - attackDistributed);
                target.setAvailableHealth(target.getAvailableHealth() - attackDistributed);
            }
            if (target.getAvailableShield() < 0.0D) {
                target.setAvailableHealth(target.getAvailableHealth() + target.getAvailableShield());
            }
            if (!target.getInitialCount().equals(target.getFinalCount())) {
                attackInformation.getUsersWithChangedCounts().add(target.getUser().getUser().getId());
            }
        } else {
            source.setPendingAttack(myAttack - victimHealth);
            if (source.getPendingAttack() > originalAttackValue) {
                source.setPendingAttack(originalAttackValue);
            }
            target.setAvailableHealth(0D);
            target.setAvailableShield(0D);
            obtainedUnitBo.delete(target.getObtainedUnit());
            deleteMissionIfRequired(attackInformation, target.getObtainedUnit());
            attackInformation.getUsersWithChangedCounts().add(target.getUser().getUser().getId());
        }
        improvementBo.clearCacheEntriesIfRequired(target.getObtainedUnit().getUnit(), obtainedUnitBo);

    }

    private void addPointsAndUpdateCount(
            AttackInformation information,
            double usedAttack, AttackObtainedUnit source,
            AttackObtainedUnit victimUnit) {
        double healthForEachUnit = Boolean.TRUE.equals(source.getObtainedUnit().getUnit().getBypassShield())
                ? victimUnit.getTotalHealth() / victimUnit.getInitialCount()
                : (victimUnit.getTotalHealth() + victimUnit.getTotalShield()) / victimUnit.getInitialCount();
        long killedCount = (long) Math.floor(usedAttack / healthForEachUnit);
        if (killedCount > victimUnit.getFinalCount()) {
            killedCount = victimUnit.getFinalCount();
            victimUnit.setFinalCount(0L);
        } else {
            victimUnit.setFinalCount(victimUnit.getFinalCount() - killedCount);
        }
        attackEventEmitter.emitAfterUnitKilledCalculation(information, source, victimUnit, killedCount);
        source.getUser().setEarnedPoints(source.getUser().getEarnedPoints() + killedCount * victimUnit.getObtainedUnit().getUnit().getPoints());
    }

    /**
     * Deletes the mission from the system, when all units involved are death
     * <p>
     * Notice, should be invoked after <b>removing the obtained unit</b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private void deleteMissionIfRequired(AttackInformation attackInformation, ObtainedUnit obtainedUnit) {
        var mission = obtainedUnit.getMission();
        if (mission != null && !obtainedUnitBo.existsByMission(mission)) {
            if (attackInformation.getAttackMission().getId().equals(mission.getId())) {
                attackInformation.setRemoved(true);
            } else {
                missionRepository.delete(mission);
                attackInformation.getUsersWithDeletedMissions().add(mission.getUser().getId());
            }
        }
    }

    private float findCriticalScore(AttackObtainedUnit attacker, AttackObtainedUnit target) {
        var unit = attacker.getObtainedUnit().getUnit();
        var criticalAttack = ObjectUtils.firstNonNull(unit.getCriticalAttack(), criticalAttackBo.findUsedCriticalAttack(unit.getType()));
        var criticalAttackRule = criticalAttackBo.findApplicableCriticalEntry(criticalAttack, target.getObtainedUnit().getUnit());
        return criticalAttackRule == null
                ? 1F
                : criticalAttackRule.getValue();
    }
}
