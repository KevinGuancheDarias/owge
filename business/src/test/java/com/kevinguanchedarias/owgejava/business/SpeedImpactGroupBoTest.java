package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.unit.util.UnitTypeInheritanceFinderService;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.kevinguanchedarias.owgejava.mock.InterceptableSpeedGroupMock.givenInterceptableSpeedGroup;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
        RequirementGroupBo.class,
        RequirementBo.class,
        ObjectRelationBo.class,
        UnlockedRelationBo.class,
        DtoUtilService.class,
        TaggableCacheManager.class,
        UnitTypeInheritanceFinderService.class
})
class SpeedImpactGroupBoTest extends AbstractBaseBoTest {
    private final SpeedImpactGroupBo speedImpactGroupBo;
    private final TaggableCacheManager taggableCacheManager;
    private final UnitTypeInheritanceFinderService unitTypeInheritanceFinderService;

    @Autowired
    public SpeedImpactGroupBoTest(
            SpeedImpactGroupBo speedImpactGroupBo,
            TaggableCacheManager taggableCacheManager,
            UnitTypeInheritanceFinderService unitTypeInheritanceFinderService
    ) {
        this.speedImpactGroupBo = speedImpactGroupBo;
        this.taggableCacheManager = taggableCacheManager;
        this.unitTypeInheritanceFinderService = unitTypeInheritanceFinderService;
    }

    @Test
    void canIntercept_should_return_true() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenUnit1();
        unit.setSpeedImpactGroup(givenSpeedImpactGroup());

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, unit)).isTrue();
        verify(unitTypeInheritanceFinderService, never()).findUnitTypeMatchingCondition(any(UnitType.class), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void canIntercept_should_use_parent_when_unit_is_null_and_return_true() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenUnit1();
        var type = unit.getType();
        type.setSpeedImpactGroup(givenSpeedImpactGroup());
        given(unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(eq(type), any()))
                .willReturn(Optional.of(type));

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, unit)).isTrue();
        var captor = ArgumentCaptor.forClass(Predicate.class);
        verify(unitTypeInheritanceFinderService, times(1)).findUnitTypeMatchingCondition(eq(type), captor.capture());
        assertThat(captor.getValue().test(type)).isTrue();
    }

    @Test
    void canIntercept_should_return_false_when_no_match() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenUnit1();
        unit.setSpeedImpactGroup(givenSpeedImpactGroup(111));

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, unit)).isFalse();
    }

    @Test
    void canIntercept_should_return_false_when_target_unit_has_no_group() {
        var interceptableSpeedGroups = List.of(givenInterceptableSpeedGroup());
        var unit = givenUnit1();

        assertThat(speedImpactGroupBo.canIntercept(interceptableSpeedGroups, unit)).isFalse();
        verify(unitTypeInheritanceFinderService, times(1)).findUnitTypeMatchingCondition(eq(unit.getType()), any());
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
