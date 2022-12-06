package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ImageStoreMock.givenImageStore;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MissionUnitsFinderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUnitRepository.class,
        ImageStoreBo.class
})
class MissionUnitsFinderBoTest {
    private final MissionUnitsFinderBo missionUnitsFinderBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ImageStoreBo imageStoreBo;

    @Autowired
    MissionUnitsFinderBoTest(
            MissionUnitsFinderBo missionUnitsFinderBo,
            ObtainedUnitRepository obtainedUnitRepository,
            ImageStoreBo imageStoreBo
    ) {
        this.missionUnitsFinderBo = missionUnitsFinderBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.imageStoreBo = imageStoreBo;
    }

    @Test
    void findUnitsInvolved_should_work() {
        var ou = givenObtainedUnit1();
        var image = givenImageStore();
        ou.getUnit().setImage(image);
        given(obtainedUnitRepository.findByMissionId(EXPLORE_MISSION_ID)).willReturn(List.of(ou));

        var retVal = missionUnitsFinderBo.findUnitsInvolved(EXPLORE_MISSION_ID);

        verify(imageStoreBo, times(1)).computeImageUrl(image);
        assertThat(retVal).hasSize(1).containsExactly(ou);
    }
}
