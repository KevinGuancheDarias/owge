package com.kevinguanchedarias.owgejava.business.speedimpactgroup;

import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenUnlockedRelation;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.SPEED_IMPACT_GROUP_ID;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = UnlockedSpeedImpactGroupService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(UnlockedRelationBo.class)
class UnlockedSpeedImpactGroupServiceTest {
    private final UnlockedSpeedImpactGroupService unlockedSpeedImpactGroupService;
    private final UnlockedRelationBo unlockedRelationBo;

    @Autowired
    UnlockedSpeedImpactGroupServiceTest(UnlockedSpeedImpactGroupService unlockedSpeedImpactGroupService, UnlockedRelationBo unlockedRelationBo) {
        this.unlockedSpeedImpactGroupService = unlockedSpeedImpactGroupService;
        this.unlockedRelationBo = unlockedRelationBo;
    }

    @Test
    void findCrossGalaxyUnlocked_should_work() {
        var user = givenUser1();
        var urList = List.of(givenUnlockedRelation(user));
        var spi = givenSpeedImpactGroup();
        given(unlockedRelationBo.findByUserIdAndObjectType(USER_ID_1, ObjectEnum.SPEED_IMPACT_GROUP)).willReturn(urList);
        given(unlockedRelationBo.unboxToTargetEntity(urList)).willReturn(List.of(spi));

        assertThat(unlockedSpeedImpactGroupService.findCrossGalaxyUnlocked(user))
                .hasSize(1)
                .contains(SPEED_IMPACT_GROUP_ID);
    }
}
