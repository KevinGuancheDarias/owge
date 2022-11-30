package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.AllianceMock.givenAlliance;
import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenUserImprovement;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenAttackMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenDeployedMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;

@UtilityClass
public class AttackMock {
    public static final int ATTACK_RULE_ID = 19281;
    public static final int CRITICAL_ATTACK_ID = 192811;
    public static final double PENDING_ATTACK = 12291;
    public static final double TOTAL_HEALTH = 14112;
    public static final double AVAILABLE_HEALTH = 14112;
    public static final double TOTAL_SHIELD = 10112;
    public static final double AVAILABLE_SHIELD = 10112;
    public static final long INITIAL_COUNT = 4;
    public static final long FINAL_COUNT = 4;
    public static final int ALLY_USER_ID = 619;
    public static final int USER_WITH_OTHER_ALLIANCE = 99928;

    public static AttackObtainedUnit givenAttackObtainedUnit(ObtainedUnit obtainedUnit) {
        return AttackObtainedUnit.builder()
                .obtainedUnit(obtainedUnit)
                .user(givenAttackUserInformation(obtainedUnit.getUser()))
                .pendingAttack(PENDING_ATTACK)
                .totalHealth(TOTAL_HEALTH)
                .totalShield(TOTAL_SHIELD)
                .availableHealth(AVAILABLE_HEALTH)
                .availableShield(AVAILABLE_SHIELD)
                .initialCount(INITIAL_COUNT)
                .finalCount(FINAL_COUNT)
                .build();
    }

    public static AttackObtainedUnit givenAttackObtainedUnit() {
        return givenAttackObtainedUnit(givenObtainedUnit1());
    }

    public static AttackInformation givenAttackInformation() {
        return AttackInformation.builder()
                .attackMission(givenAttackMission())
                .targetPlanet(givenTargetPlanet())
                .build();
    }

    public static AttackUserInformation givenAttackUserInformation(UserStorage user) {
        return new AttackUserInformation(user, givenUserImprovement());
    }

    public static AttackUserInformation givenAttackUserInformation(UserStorage user, AttackObtainedUnit attackObtainedUnit) {
        var userInformation = givenAttackUserInformation(user);
        userInformation.getUnits().add(attackObtainedUnit);
        return userInformation;
    }

    public static AttackInformation givenFullAttackInformation() {
        AttackInformation information = givenAttackInformation();
        var ou1 = givenAttackObtainedUnit();
        ou1.getObtainedUnit().setMission(givenAttackMission());
        var ou2 = givenAttackObtainedUnit(givenObtainedUnit2());
        ou2.getObtainedUnit().setMission(givenDeployedMission());
        information.getUnits().addAll(List.of(ou1, ou2));
        information.getUsers().put(USER_ID_1, givenAttackUserInformation(givenUser1(), ou1));
        information.getUsers().put(USER_ID_2, givenAttackUserInformation(givenUser2(), ou2));
        return information;
    }

    public static AttackInformation givenFullAttackInformationWithAlly() {
        AttackInformation information = givenAttackInformation();
        var ou1 = givenAttackObtainedUnit();
        ou1.getObtainedUnit().setMission(givenAttackMission());
        var alliance = givenAlliance();
        var userWithAlliance = givenUser2().toBuilder().alliance(alliance).build();
        var theAlly = givenUser(ALLY_USER_ID).toBuilder().alliance(alliance).build();
        var userWithOtherAlliance = givenUser(USER_WITH_OTHER_ALLIANCE);
        var ou2 = givenAttackObtainedUnit(givenObtainedUnit2());
        ou2.setFinalCount(0L);
        ou2.getObtainedUnit().setMission(givenDeployedMission());
        var ou3 = givenAttackObtainedUnit(givenObtainedUnit2());
        ou3.getObtainedUnit().setMission(givenDeployedMission());
        var ou4 = givenAttackObtainedUnit();
        ou4.getObtainedUnit().setMission(givenDeployedMission());
        information.getUnits().addAll(List.of(ou1, ou2, ou3));
        information.getUsers().put(USER_ID_1, givenAttackUserInformation(givenUser1(), ou1));
        information.getUsers().put(USER_ID_2, givenAttackUserInformation(userWithAlliance, ou2));
        information.getUsers().put(ALLY_USER_ID, givenAttackUserInformation(theAlly, ou3));
        information.getUsers().put(USER_WITH_OTHER_ALLIANCE, givenAttackUserInformation(userWithOtherAlliance, ou4));
        return information;
    }

    public static AttackRule givenAttackRule() {
        return AttackRule.builder()
                .id(ATTACK_RULE_ID)
                .build();
    }

    public static CriticalAttack givenCriticalAttack() {
        return CriticalAttack.builder()
                .id(CRITICAL_ATTACK_ID)
                .build();
    }

    public static CriticalAttackEntry givenCriticalAttackEntry(float multiplier) {
        return CriticalAttackEntry.builder()
                .id(RandomUtils.nextInt())
                .value(multiplier)
                .build();

    }
}
