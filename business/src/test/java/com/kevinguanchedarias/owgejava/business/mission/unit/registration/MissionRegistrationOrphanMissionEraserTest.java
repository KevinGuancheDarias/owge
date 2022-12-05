package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.*;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = MissionRegistrationOrphanMissionEraser.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(ObtainedUnitRepository.class)
class MissionRegistrationOrphanMissionEraserTest {
    private final MissionRegistrationOrphanMissionEraser missionRegistrationOrphanMissionEraser;
    private final ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    MissionRegistrationOrphanMissionEraserTest(
            MissionRegistrationOrphanMissionEraser missionRegistrationOrphanMissionEraser,
            ObtainedUnitRepository obtainedUnitRepository
    ) {
        this.missionRegistrationOrphanMissionEraser = missionRegistrationOrphanMissionEraser;
        this.obtainedUnitRepository = obtainedUnitRepository;
    }

    @ParameterizedTest
    @MethodSource("doMarkAsDeletedOrphanMissions_should_work_arguments")
    void doMarkAsDeletedOrphanMissions_should_work(Mission unitMission, boolean expectedIsResolved) {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        ou.setMission(unitMission);
        given(obtainedUnitRepository.findByMissionIdIn(List.of(EXPLORE_MISSION_ID))).willReturn(List.of(ou));

        missionRegistrationOrphanMissionEraser.doMarkAsDeletedTheOrphanMissions(Set.of(mission));

        assertThat(mission.getResolved()).isEqualTo(expectedIsResolved);

    }

    private static Stream<Arguments> doMarkAsDeletedOrphanMissions_should_work_arguments() {
        return Stream.of(
                Arguments.of(givenExploreMission(), false),
                Arguments.of(null, true),
                Arguments.of(givenGatherMission(), true)
        );
    }
}
