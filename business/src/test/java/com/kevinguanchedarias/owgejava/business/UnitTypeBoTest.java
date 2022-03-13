package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.mock.FactionMock;
import com.kevinguanchedarias.owgejava.repository.FactionUnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenUserImprovement;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenEntity;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitTypeBo.class
)
@MockBean({
        UnitTypeRepository.class,
        ImprovementBo.class,
        UnitMissionBo.class,
        UserStorageBo.class,
        ObtainedUnitRepository.class,
        SocketIoService.class,
        FactionUnitTypeRepository.class,
        ObtainedUnitBo.class
})
class UnitTypeBoTest {
    private final UnitTypeBo unitTypeBo;
    private final UnitTypeRepository repository;
    private final UserStorageBo userStorageBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ImprovementBo improvementBo;

    @Autowired
    UnitTypeBoTest(
            UnitTypeBo unitTypeBo,
            UnitTypeRepository repository,
            UserStorageBo userStorageBo,
            ObtainedUnitBo obtainedUnitBo,
            ImprovementBo improvementBo
    ) {
        this.unitTypeBo = unitTypeBo;
        this.repository = repository;
        this.userStorageBo = userStorageBo;
        this.obtainedUnitBo = obtainedUnitBo;
        this.improvementBo = improvementBo;
    }

    @Test
    void findUnitTypesWithUserInfo_should_work() {
        var type = givenEntity();
        type.setSpeedImpactGroup(givenSpeedImpactGroup());
        var typeName = "foo";
        type.setName(typeName);
        var maxCount = 19L;
        type.setMaxCount(maxCount);
        var user = givenUser1();
        user.setFaction(FactionMock.givenEntity());
        var userImprovement = givenUserImprovement();
        var maxAfterImprovement = 25D;
        var built = 27L;
        var isUsed = true;
        given(repository.findAll()).willReturn(List.of(type));
        given(userStorageBo.findById(USER_ID_1)).willReturn(user);
        given(improvementBo.findUserImprovement(user)).willReturn(userImprovement);
        given(improvementBo.computeImprovementValue(anyDouble(), anyDouble())).willReturn(maxAfterImprovement);
        given(obtainedUnitBo.countByUserAndUnitType(user, type)).willReturn(built);
        given(repository.existsByUnitsTypeId(UNIT_TYPE_ID)).willReturn(isUsed);

        var result = unitTypeBo.findUnitTypesWithUserInfo(USER_ID_1);

        verify(userStorageBo, times(1)).findById(USER_ID_1);
        verify(improvementBo, times(1)).findUserImprovement(user);
        verify(improvementBo, times(1)).computeImprovementValue(anyDouble(), anyDouble());
        verify(obtainedUnitBo, times(1)).countByUserAndUnitType(user, type);
        assertThat(result).hasSize(1);
        var resultEntry = result.get(0);
        assertThat(resultEntry.isUsed()).isTrue();
        assertThat(resultEntry.getUserBuilt()).isEqualTo(built);
        assertThat(resultEntry.getComputedMaxCount()).isEqualTo(Double.valueOf(Math.floor(maxAfterImprovement)).longValue());
        assertThat(resultEntry.getName()).isEqualTo(typeName);

    }
}
