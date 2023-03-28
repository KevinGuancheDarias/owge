package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.InterceptableSpeedGroupMock.givenInterceptableSpeedGroup;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_ID;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = SpeedImpactGroupBo.class
)
@MockBean({
        SpeedImpactGroupRepository.class,
        ObjectRelationToObjectRelationRepository.class,
        RequirementBo.class,
        ObjectRelationBo.class,
        DtoUtilService.class,
        SpeedImpactGroupFinderBo.class,
        RequirementGroupRepository.class,
        ObtainedUnitRepository.class
})
class SpeedImpactGroupBoTest {
    private final SpeedImpactGroupBo speedImpactGroupBo;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;
    private final ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    public SpeedImpactGroupBoTest(
            SpeedImpactGroupBo speedImpactGroupBo,
            SpeedImpactGroupFinderBo speedImpactGroupFinderBo,
            ObtainedUnitRepository obtainedUnitRepository
    ) {
        this.speedImpactGroupBo = speedImpactGroupBo;
        this.speedImpactGroupFinderBo = speedImpactGroupFinderBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
    }

    @Test
    void canIntercept_should_return_true() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenObtainedUnit1();
        var user = givenUser1();
        given(speedImpactGroupFinderBo.findApplicable(user, unit.getUnit())).willReturn(givenSpeedImpactGroup());

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, user, unit)).isTrue();
        verify(obtainedUnitRepository, never()).findUnitByOuId(any());
    }

    @Test
    void canIntercept_should_return_false_when_no_match() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenObtainedUnit1();
        var user = givenUser1();
        given(speedImpactGroupFinderBo.findApplicable(user, unit.getUnit())).willReturn(givenSpeedImpactGroup(111));

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, user, unit)).isFalse();
    }

    @Test
    void canIntercept_should_return_false_when_target_unit_has_no_group() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenObtainedUnit1();
        unit.setOwnerUnit(new ObtainedUnit());

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, givenUser1(), unit)).isFalse();
    }

    @Test
    void canIntercept_should_user_owner_unit_when_it_has() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var ou = givenObtainedUnit1();
        var ownerUnit = new Unit();
        ownerUnit.setId(192814);
        var user = ou.getUser();
        var ownerOu = ou.toBuilder()
                .id(OBTAINED_UNIT_1_ID + 1)
                .unit(ownerUnit)
                .build();
        ou.setOwnerUnit(ownerOu);
        given(speedImpactGroupFinderBo.findApplicable(eq(user), any())).willReturn(givenSpeedImpactGroup(111));
        given(obtainedUnitRepository.findUnitByOuId(ownerOu.getId())).willReturn(ownerUnit);

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, user, ou)).isFalse();

        verify(obtainedUnitRepository, times(1)).findUnitByOuId(ownerOu.getId());
        verify(speedImpactGroupFinderBo, times(1)).findApplicable(user, ownerUnit);
    }
}
