package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.business.audit.AuditBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MissionRegistrationAuditor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        AuditBo.class,
        ObtainedUnitFinderBo.class
})
class MissionRegistrationAuditorTest {
    private final MissionRegistrationAuditor missionRegistrationAuditor;
    private final AuditBo auditBo;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;

    @Autowired
    MissionRegistrationAuditorTest(
            MissionRegistrationAuditor missionRegistrationAuditor,
            AuditBo auditBo,
            ObtainedUnitFinderBo obtainedUnitFinderBo
    ) {
        this.missionRegistrationAuditor = missionRegistrationAuditor;
        this.auditBo = auditBo;
        this.obtainedUnitFinderBo = obtainedUnitFinderBo;
    }

    @ParameterizedTest
    @MethodSource("auditMissionRegistration_should_work_arguments")
    void auditMissionRegistration_should_work(
            UserStorage planetOwner,
            ObtainedUnit ou,
            boolean isDeploy,
            int timesDoAuditWithRelated,
            int timesDoAuditWithoutRelated,
            int timesObtainedUnitAudit,
            int timesAuditUnit
    ) {
        var mission = givenExploreMission();
        mission.setUser(givenUser1());
        var missionTypeCode = MissionType.EXPLORE.name();
        var targetPlanet = mission.getTargetPlanet();
        targetPlanet.setOwner(planetOwner);
        given(obtainedUnitFinderBo.findInPlanetOrInMissionToPlanet(targetPlanet)).willReturn(List.of(ou));

        missionRegistrationAuditor.auditMissionRegistration(mission, isDeploy);

        verify(auditBo, times(timesDoAuditWithoutRelated)).doAudit(AuditActionEnum.REGISTER_MISSION, missionTypeCode, null);
        verify(auditBo, times(timesDoAuditWithRelated)).doAudit(AuditActionEnum.REGISTER_MISSION, missionTypeCode, USER_ID_2);
        verify(obtainedUnitFinderBo, times(timesObtainedUnitAudit)).findInPlanetOrInMissionToPlanet(targetPlanet);
        verify(auditBo, times(timesAuditUnit)).doAudit(AuditActionEnum.USER_INTERACTION, "DEPLOY", USER_ID_2);
    }

    private static Stream<Arguments> auditMissionRegistration_should_work_arguments() {
        var planetOwnerIsSame = givenUser1();
        var planetOwnerIsOther = givenUser2();
        var ouOfSameUser = givenObtainedUnit1();
        var ouOfOtherUser = givenObtainedUnit2();
        return Stream.of(
                Arguments.of(planetOwnerIsSame, ouOfSameUser, false, 0, 1, 0, 0),
                Arguments.of(null, ouOfSameUser, false, 0, 1, 0, 0),
                Arguments.of(planetOwnerIsOther, ouOfSameUser, false, 1, 0, 0, 0),
                Arguments.of(planetOwnerIsSame, ouOfSameUser, true, 0, 1, 1, 0),
                Arguments.of(planetOwnerIsOther, ouOfOtherUser, true, 1, 0, 1, 1)
        );
    }
}
