package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.InterceptableSpeedGroupMock.givenInterceptableSpeedGroup;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = SpeedImpactGroupBo.class
)
@MockBean({
        SpeedImpactGroupRepository.class,
        ObjectRelationToObjectRelationRepository.class,
        RequirementGroupBo.class,
        RequirementBo.class,
        ObjectRelationBo.class,
        UnlockedRelationBo.class,
        DtoUtilService.class,
        TaggableCacheManager.class,
        SpeedImpactGroupFinderBo.class
})
class SpeedImpactGroupBoTest extends AbstractBaseBoTest {
    private final SpeedImpactGroupBo speedImpactGroupBo;
    private final TaggableCacheManager taggableCacheManager;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;

    @Autowired
    public SpeedImpactGroupBoTest(
            SpeedImpactGroupBo speedImpactGroupBo,
            TaggableCacheManager taggableCacheManager,
            SpeedImpactGroupFinderBo speedImpactGroupFinderBo) {
        this.speedImpactGroupBo = speedImpactGroupBo;
        this.taggableCacheManager = taggableCacheManager;
        this.speedImpactGroupFinderBo = speedImpactGroupFinderBo;
    }

    @Test
    void canIntercept_should_return_true() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenUnit1();
        var user = givenUser1();
        given(speedImpactGroupFinderBo.findApplicable(user, unit)).willReturn(givenSpeedImpactGroup());

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, user, unit)).isTrue();
    }

    @Test
    void canIntercept_should_return_false_when_no_match() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenUnit1();
        var user = givenUser1();
        given(speedImpactGroupFinderBo.findApplicable(user, unit)).willReturn(givenSpeedImpactGroup(111));

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, user, unit)).isFalse();
    }

    @Test
    void canIntercept_should_return_false_when_target_unit_has_no_group() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenUnit1();

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, givenUser1(), unit)).isFalse();
    }


    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(SpeedImpactGroupBo.SPEED_IMPACT_GROUP_CACHE_TAG)
                .targetBo(speedImpactGroupBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
