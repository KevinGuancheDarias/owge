package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.*;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitImprovementCalculationService;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.exception.OwgeElementSideDeletedException;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.function.Supplier;

import static com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter.UNIT_OBTAINED_CHANGE;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.*;
import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenUserImprovement;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.ATTACK_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenAttackMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.*;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenUnitType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = AttackMissionManagerBo.class
)
@MockBean({
        ObtainedUnitBo.class,
        ImprovementBo.class,
        AttackObtainedUnitBo.class,
        AttackRuleBo.class,
        CriticalAttackBo.class,
        MissionRepository.class,
        UserStorageBo.class,
        UnitTypeBo.class,
        SocketIoService.class,
        MissionBo.class,
        AllianceBo.class,
        AttackEventEmitter.class,
        ObtainedUnitRepository.class,
        MissionEventEmitterBo.class,
        UserEventEmitterBo.class,
        UserStorageRepository.class,
        ObtainedUnitEventEmitter.class,
        TransactionUtilService.class,
        ObtainedUnitImprovementCalculationService.class
})
class AttackMissionManagerBoTest {
    private final AttackMissionManagerBo attackMissionManagerBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ImprovementBo improvementBo;
    private final AttackObtainedUnitBo attackObtainedUnitBo;
    private final AttackRuleBo attackRuleBo;
    private final CriticalAttackBo criticalAttackBo;
    private final SocketIoService socketIoService;
    private final AllianceBo allianceBo;
    private final AttackEventEmitter attackEventEmitter;
    private final ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    public AttackMissionManagerBoTest(
            AttackMissionManagerBo attackMissionManagerBo,
            ObtainedUnitBo obtainedUnitBo,
            ImprovementBo improvementBo,
            AttackObtainedUnitBo attackObtainedUnitBo,
            AttackRuleBo attackRuleBo,
            CriticalAttackBo criticalAttackBo,
            SocketIoService socketIoService,
            AllianceBo allianceBo,
            AttackEventEmitter attackEventEmitter,
            ObtainedUnitRepository obtainedUnitRepository
    ) {
        this.attackMissionManagerBo = attackMissionManagerBo;
        this.obtainedUnitBo = obtainedUnitBo;
        this.improvementBo = improvementBo;
        this.attackObtainedUnitBo = attackObtainedUnitBo;
        this.attackRuleBo = attackRuleBo;
        this.criticalAttackBo = criticalAttackBo;
        this.socketIoService = socketIoService;
        this.allianceBo = allianceBo;

        this.attackEventEmitter = attackEventEmitter;
        this.obtainedUnitRepository = obtainedUnitRepository;
    }

    @Test
    void buildAttackInformation_should_work() {
        var attackMission = givenAttackMission();
        var targetPlanet = givenTargetPlanet();
        when(obtainedUnitBo.findInvolvedInAttack(targetPlanet)).thenReturn(List.of(givenObtainedUnit1()));
        when(obtainedUnitRepository.findByMissionId(ATTACK_MISSION_ID)).thenReturn(List.of(givenObtainedUnit2()));
        when(improvementBo.findUserImprovement(givenUser1())).thenReturn(givenUserImprovement());
        when(improvementBo.findUserImprovement(givenUser2())).thenReturn(givenUserImprovement());
        var attackOu1 = givenAttackObtainedUnit(givenObtainedUnit1());
        var attackOu2 = givenAttackObtainedUnit(givenObtainedUnit2());
        when(attackObtainedUnitBo.create(eq(givenObtainedUnit1()), any())).thenReturn(attackOu1);
        when(attackObtainedUnitBo.create(eq(givenObtainedUnit2()), any())).thenReturn(attackOu2);

        var information = attackMissionManagerBo.buildAttackInformation(targetPlanet, attackMission);

        verify(obtainedUnitBo, times(1)).findInvolvedInAttack(targetPlanet);
        verify(obtainedUnitRepository, times(1)).findByMissionId(ATTACK_MISSION_ID);
        var attackUserInformation1 = ArgumentCaptor.forClass(AttackUserInformation.class);
        var attackUserInformation2 = ArgumentCaptor.forClass(AttackUserInformation.class);
        verify(attackObtainedUnitBo, times(1)).create(eq(givenObtainedUnit1()), attackUserInformation1.capture());
        verify(attackObtainedUnitBo, times(1)).create(eq(givenObtainedUnit2()), attackUserInformation2.capture());

        assertThat(information.getAttackMission()).isEqualTo(attackMission);
        assertThat(information.getTargetPlanet()).isEqualTo(targetPlanet);
        assertThat(information.getUsers()).hasSize(2);
        assertThat(information.getUnits()).hasSize(2);
        assertThat(information.getUnits()).containsAll(List.of(attackOu1, attackOu2));
        assertThat(information.getUsers())
                .containsEntry(USER_ID_1, attackUserInformation1.getValue())
                .containsEntry(USER_ID_2, attackUserInformation2.getValue());
    }

    @Test
    void addUnit_should_add_unit_to_attack_information_and_create_user_entry_if_missing() {
        var information = givenAttackInformation();
        when(this.attackObtainedUnitBo.create(eq(givenObtainedUnit1()), any())).thenReturn(givenAttackObtainedUnit());

        attackMissionManagerBo.addUnit(information, givenObtainedUnit1());

        var attackUserInformation = ArgumentCaptor.forClass(AttackUserInformation.class);
        verify(attackObtainedUnitBo, times(1)).create(eq(givenObtainedUnit1()), attackUserInformation.capture());

        assertThat(information.getUsers()).hasSize(1);
        assertThat(information.getUnits()).hasSize(1);
        assertThat(information.getUsers())
                .containsEntry(USER_ID_1, attackUserInformation.getValue());
        assertThat(information.getUnits()).contains(givenAttackObtainedUnit());
    }

    @Test
    void addUnit_should_add_unit_to_attack_information_and_append_to_existing_user() {
        var information = givenAttackInformation();
        information.getUnits().add(givenAttackObtainedUnit());
        information.getUsers().put(USER_ID_1, givenAttackUserInformation(givenUser1(), givenAttackObtainedUnit()));
        information.getUsers().get(USER_ID_1).getUnits().add(givenAttackObtainedUnit());
        when(this.attackObtainedUnitBo.create(eq(givenObtainedUnit1()), any())).thenReturn(givenAttackObtainedUnit());

        attackMissionManagerBo.addUnit(information, givenObtainedUnit1());

        var attackUserInformation = ArgumentCaptor.forClass(AttackUserInformation.class);
        verify(attackObtainedUnitBo, times(1)).create(eq(givenObtainedUnit1()), attackUserInformation.capture());

        assertThat(information.getUsers()).hasSize(1);
        assertThat(information.getUnits()).hasSize(2);
        assertThat(information.getUsers())
                .containsEntry(USER_ID_1, attackUserInformation.getValue());
        assertThat(information.getUnits()).contains(givenAttackObtainedUnit());
    }

    @Test
    void startAttack_should_work(CapturedOutput capturedOutput) {
        AttackInformation information = givenFullAttackInformation();
        var user1 = information.getUsers().get(USER_ID_1);
        var user2 = information.getUsers().get(USER_ID_2);
        information.getTargetPlanet().setOwner(user1.getUser());
        var survivorUnit = AttackObtainedUnit.builder()
                .obtainedUnit(givenObtainedUnit1())
                .user(user1)
                .availableHealth(Double.MAX_VALUE / 4)
                .totalHealth(Double.MAX_VALUE / 4)
                .availableShield(0D)
                .totalShield(0D)
                .initialCount(Long.MAX_VALUE)
                .finalCount(Long.MAX_VALUE)
                .pendingAttack(Double.MAX_VALUE / 1e3)
                .build();
        var survivorUnit2 = survivorUnit.toBuilder()
                .user(user2)
                .obtainedUnit(givenObtainedUnit2())
                .build();
        var attackableInmortalUnit = AttackObtainedUnit.builder()
                .obtainedUnit(givenObtainedUnit1())
                .user(user1)
                .availableHealth(Double.MAX_VALUE)
                .totalHealth(Double.MAX_VALUE)
                .availableShield(0D)
                .totalShield(0D)
                .pendingAttack(0D)
                .initialCount(4L)
                .finalCount(4L)
                .build();
        var withBypassShields = AttackObtainedUnit.builder()
                .obtainedUnit(givenObtainedUnitWithBypassShields(user2.getUser()))
                .user(user2)
                .availableHealth(10D)
                .totalHealth(10D)
                .availableShield(0D)
                .totalShield(0D)
                .pendingAttack(50000D)
                .finalCount(8L)
                .initialCount(8L)
                .build();
        var bypassShieldWithALotOfAttack = withBypassShields.toBuilder()
                .obtainedUnit(givenObtainedUnit2())
                .totalShield(8D)
                .availableShield(8D)
                .pendingAttack(Double.MAX_VALUE / 1e3)
                .build();
        survivorUnit.getObtainedUnit().getUnit().setType(new UnitType());
        information.getUnits().addAll(List.of(survivorUnit, survivorUnit2, attackableInmortalUnit, withBypassShields, bypassShieldWithALotOfAttack));
        information.getUsers().get(USER_ID_1).getUnits().addAll(List.of(attackableInmortalUnit, survivorUnit));
        information.getUsers().get(USER_ID_2).getUnits().addAll(List.of(withBypassShields, bypassShieldWithALotOfAttack, survivorUnit2));
        var ou1 = information.getUnits().get(0);
        ou1.setUser(user1);
        ou1.getObtainedUnit().getUnit().setAttackRule(AttackRule.builder().id(1122).build());
        var ou2 = information.getUnits().get(1);
        ou2.setUser(user2);
        var attackRule = givenAttackRule();
        var unitType = givenUnitType(UNIT_TYPE_ID);
        var criticalMultiplier = 18;
        var fakedToDtoOfindDeployedInUserOwnedPlanets = new ObtainedUnitDto();
        var fakedFindDeployedInUserOwnedPlanets = new ObtainedUnit();
        when(allianceBo.areEnemies(user1.getUser(), user2.getUser())).thenReturn(true);
        when(allianceBo.areEnemies(user2.getUser(), user1.getUser())).thenReturn(true);
        when(attackRuleBo.findAttackRule(unitType)).thenReturn(attackRule);
        when(attackRuleBo.canAttack(attackRule, ou1.getObtainedUnit())).thenReturn(true);
        when(attackRuleBo.canAttack(any(), eq(withBypassShields.getObtainedUnit()))).thenReturn(true);
        when(attackRuleBo.canAttack(any(), eq(bypassShieldWithALotOfAttack.getObtainedUnit()))).thenReturn(true);
        when(criticalAttackBo.findUsedCriticalAttack(unitType)).thenReturn(givenCriticalAttack());
        when(criticalAttackBo.findApplicableCriticalEntry(eq(givenCriticalAttack()), any(Unit.class)))
                .thenReturn(givenCriticalAttackEntry(criticalMultiplier));
        when(obtainedUnitBo.saveWithChange(eq(survivorUnit.getObtainedUnit()), anyLong())).thenThrow(new OwgeElementSideDeletedException("foo"));
        doAnswer(answer -> {
            answer.getArgument(2, Supplier.class).get();
            return null;
        }).when(socketIoService).sendMessage(any(), eq(UNIT_OBTAINED_CHANGE), any());
        when(obtainedUnitRepository.findDeployedInUserOwnedPlanets(any())).thenReturn(List.of(fakedFindDeployedInUserOwnedPlanets));
        when(obtainedUnitBo.toDto(anyList())).thenReturn(List.of(fakedToDtoOfindDeployedInUserOwnedPlanets));

        try (var transactionUtilMock = mockStatic(TransactionUtil.class)) {
            transactionUtilMock.when(() -> TransactionUtil.doAfterCommit(any())).thenAnswer(invocationOnMock -> {
                var runnable = invocationOnMock.getArgument(0, Runnable.class);
                runnable.run();
                return null;
            });

            attackMissionManagerBo.startAttack(information);

            verify(attackObtainedUnitBo, times(1)).shuffleUnits(information.getUnits());
            verify(obtainedUnitRepository, times(2)).delete(any(ObtainedUnit.class));
            verify(attackEventEmitter, times(9)).emitAfterUnitKilledCalculation(any(), any(), any(), anyLong());
            assertThat(capturedOutput.getOut()).contains("Element side deleted");
            assertThat(user1.getAttackableUnits())
                    .hasSize(4)
                    .contains(ou2);
            assertThat(user2.getAttackableUnits())
                    .hasSize(3)
                    .contains(ou1);
        }
    }
}
