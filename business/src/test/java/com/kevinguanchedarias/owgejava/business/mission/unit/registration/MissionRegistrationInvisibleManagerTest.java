package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MissionRegistrationInvisibleManager.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        HiddenUnitBo.class,
        MissionRepository.class,
        ObtainedUnitRepository.class
})
class MissionRegistrationInvisibleManagerTest {
    private final MissionRegistrationInvisibleManager missionRegistrationInvisibleManager;
    private final HiddenUnitBo hiddenUnitBo;
    private final MissionRepository missionRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    MissionRegistrationInvisibleManagerTest(
            MissionRegistrationInvisibleManager missionRegistrationInvisibleManager,
            HiddenUnitBo hiddenUnitBo,
            MissionRepository missionRepository,
            ObtainedUnitRepository obtainedUnitRepository
    ) {
        this.missionRegistrationInvisibleManager = missionRegistrationInvisibleManager;
        this.hiddenUnitBo = hiddenUnitBo;
        this.missionRepository = missionRepository;
        this.obtainedUnitRepository = obtainedUnitRepository;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handleDefineMissionAsInvisible_should_work(boolean isHiddenUnit) {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        given(hiddenUnitBo.isHiddenUnit(ou.getUser(), ou.getUnit())).willReturn(isHiddenUnit);

        missionRegistrationInvisibleManager.handleDefineMissionAsInvisible(mission, List.of(ou));

        assertThat(mission.getInvisible()).isEqualTo(isHiddenUnit);
    }

    @ParameterizedTest
    @MethodSource("maybeUpdateMissionsVisibility_should_work_arguments")
    void maybeUpdateMissionsVisibility_should_work(
            boolean initialIsInvisible,
            boolean newVisibilityValue,
            int timesSave
    ) {
        var mission = givenExploreMission();
        mission.setInvisible(initialIsInvisible);
        var ou = givenObtainedUnit1();
        given(obtainedUnitRepository.findByMissionId(EXPLORE_MISSION_ID)).willReturn(List.of(ou));
        given(hiddenUnitBo.isHiddenUnit(ou.getUser(), ou.getUnit())).willReturn(newVisibilityValue);

        missionRegistrationInvisibleManager.maybeUpdateMissionsVisibility(List.of(mission));

        assertThat(mission.getInvisible()).isEqualTo(newVisibilityValue);
        verify(missionRepository, times(timesSave)).save(mission);
    }

    private static Stream<Arguments> maybeUpdateMissionsVisibility_should_work_arguments() {
        return Stream.of(
                Arguments.of(false, true, 1),
                Arguments.of(true, true, 0),
                Arguments.of(true, false, 1)
        );
    }
}
