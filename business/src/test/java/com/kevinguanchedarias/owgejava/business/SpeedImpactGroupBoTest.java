package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
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
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        ObtainedUnitFinderBo.class
})
class SpeedImpactGroupBoTest {
    private final SpeedImpactGroupBo speedImpactGroupBo;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;

    @Autowired
    public SpeedImpactGroupBoTest(
            SpeedImpactGroupBo speedImpactGroupBo,
            SpeedImpactGroupFinderBo speedImpactGroupFinderBo,
            ObtainedUnitFinderBo obtainedUnitFinderBo
    ) {
        this.speedImpactGroupBo = speedImpactGroupBo;
        this.speedImpactGroupFinderBo = speedImpactGroupFinderBo;
        this.obtainedUnitFinderBo = obtainedUnitFinderBo;
    }

    @Test
    void canIntercept_should_return_true() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var ou = givenObtainedUnit1();
        var user = givenUser1();
        given(speedImpactGroupFinderBo.findApplicable(user, ou.getUnit())).willReturn(givenSpeedImpactGroup());
        given(obtainedUnitFinderBo.determineTargetUnit(ou)).willReturn(ou.getUnit());

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, user, ou)).isTrue();
        verify(obtainedUnitFinderBo, times(1)).determineTargetUnit(ou);
    }

    @Test
    void canIntercept_should_return_false_when_no_match() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var ou = givenObtainedUnit1();
        var user = givenUser1();
        given(speedImpactGroupFinderBo.findApplicable(user, ou.getUnit())).willReturn(givenSpeedImpactGroup(111));
        given(obtainedUnitFinderBo.determineTargetUnit(ou)).willReturn(ou.getUnit());

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, user, ou)).isFalse();

        verify(obtainedUnitFinderBo, times(1)).determineTargetUnit(ou);
    }

    @Test
    void canIntercept_should_return_false_when_target_unit_has_no_group() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var ou = givenObtainedUnit1();
        ou.setOwnerUnit(new ObtainedUnit());
        given(obtainedUnitFinderBo.determineTargetUnit(ou)).willReturn(ou.getUnit());

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, givenUser1(), ou)).isFalse();

        verify(obtainedUnitFinderBo, times(1)).determineTargetUnit(ou);
    }
}
