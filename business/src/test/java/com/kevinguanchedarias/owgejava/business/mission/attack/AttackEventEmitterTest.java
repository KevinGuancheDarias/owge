package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef.AfterAttackEndListener;
import com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef.AfterUnitKilledCalculationListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenAttackInformation;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenAttackObtainedUnit;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = AttackEventEmitter.class
)
@MockBean({
        AfterUnitKilledCalculationListener.class,
        AfterAttackEndListener.class
})
class AttackEventEmitterTest {
    private final AttackEventEmitter attackEventEmitter;
    private final AfterUnitKilledCalculationListener afterUnitKilledCalculationListener;
    private final AfterAttackEndListener afterAttackEndListener;


    @Autowired
    public AttackEventEmitterTest(
            AttackEventEmitter attackEventEmitter,
            AfterUnitKilledCalculationListener afterUnitKilledCalculationListener,
            AfterAttackEndListener afterAttackEndListener
    ) {
        this.attackEventEmitter = attackEventEmitter;
        this.afterUnitKilledCalculationListener = afterUnitKilledCalculationListener;
        this.afterAttackEndListener = afterAttackEndListener;
    }

    @Test
    void emitAfterUnitKilledCalculation_should_invoke_listeners() {
        var attacker = givenAttackObtainedUnit(givenObtainedUnit1());
        var victim = givenAttackObtainedUnit(givenObtainedUnit2());
        var killed = 2L;

        attackEventEmitter.emitAfterUnitKilledCalculation(givenAttackInformation(), attacker, victim, killed);

        verify(afterUnitKilledCalculationListener, times(1))
                .onAfterUnitKilledCalculation(givenAttackInformation(), attacker, victim, killed);
    }

    @Test
    void emitAttackEnd_should_invoke_listeners() {
        var information = givenAttackInformation();

        attackEventEmitter.emitAttackEnd(information);

        verify(afterAttackEndListener, times(1)).onAttackEnd(information);
    }
}
