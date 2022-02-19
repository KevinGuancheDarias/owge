package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenAttackInformation;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_COUNT;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = AttackObtainedUnitBo.class
)
@MockBean(
        ImprovementBo.class
)
class AttackObtainedUnitBoTest {
    private final AttackObtainedUnitBo attackObtainedUnitBo;
    private final ImprovementBo improvementBo;

    @Autowired
    public AttackObtainedUnitBoTest(AttackObtainedUnitBo attackObtainedUnitBo, ImprovementBo improvementBo) {
        this.attackObtainedUnitBo = attackObtainedUnitBo;
        this.improvementBo = improvementBo;
    }

    @Test
    void create_should_work() {
        var ou = givenObtainedUnit1();
        var unitType = ou.getUnit().getType();
        var information = givenAttackInformation();
        var improvementMock = mock(GroupedImprovement.class);
        var userInformation = new AttackUserInformation(givenUser1(), improvementMock);
        information.getUsers().put(USER_ID_1, userInformation);
        when(improvementMock.findUnitTypeImprovement(ImprovementTypeEnum.ATTACK, unitType))
                .thenReturn(10L);
        when(improvementMock.findUnitTypeImprovement(ImprovementTypeEnum.SHIELD, unitType))
                .thenReturn(20L);
        when(improvementMock.findUnitTypeImprovement(ImprovementTypeEnum.DEFENSE, unitType))
                .thenReturn(30L);
        when(improvementBo.findAsRational(anyDouble()))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, Double.class) / 100D);

        var attackObtainedUnit = this.attackObtainedUnitBo.create(ou, userInformation);

        assertThat(attackObtainedUnit.getObtainedUnit()).isEqualTo(ou);
        assertThat(attackObtainedUnit.getUser()).isEqualTo(userInformation);
        assertThat(attackObtainedUnit.getInitialCount()).isEqualTo(ou.getCount());
        assertThat(attackObtainedUnit.getFinalCount()).isEqualTo(ou.getCount());
        assertThat(attackObtainedUnit.getPendingAttack())
                .isEqualTo(OBTAINED_UNIT_1_COUNT * ou.getUnit().getAttack() + 18);
        assertThat(attackObtainedUnit.getTotalHealth())
                .isEqualTo(OBTAINED_UNIT_1_COUNT * ou.getUnit().getHealth() + 114);
        assertThat(attackObtainedUnit.getTotalShield())
                .isEqualTo(OBTAINED_UNIT_1_COUNT * ou.getUnit().getShield() + 56);
    }
}
