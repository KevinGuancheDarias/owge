package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.checker.EntityCanDoMissionChecker;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.mock.FactionMock;
import com.kevinguanchedarias.owgejava.repository.FactionUnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenUserImprovement;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenUnitType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
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
        ObtainedUnitBo.class,
        TaggableCacheManager.class,
        EntityCanDoMissionChecker.class
})
class UnitTypeBoTest extends AbstractBaseBoTest {
    private static final int SECOND_UNIT_TYPE_ID = 11811;

    private final UnitTypeBo unitTypeBo;
    private final UnitTypeRepository repository;
    private final UserStorageBo userStorageBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ImprovementBo improvementBo;
    private final TaggableCacheManager taggableCacheManager;
    private final EntityCanDoMissionChecker entityCanDoMissionChecker;

    @Autowired
    UnitTypeBoTest(
            UnitTypeBo unitTypeBo,
            UnitTypeRepository repository,
            UserStorageBo userStorageBo,
            ObtainedUnitBo obtainedUnitBo,
            ImprovementBo improvementBo,
            TaggableCacheManager taggableCacheManager,
            EntityCanDoMissionChecker entityCanDoMissionChecker
    ) {
        this.unitTypeBo = unitTypeBo;
        this.repository = repository;
        this.userStorageBo = userStorageBo;
        this.obtainedUnitBo = obtainedUnitBo;
        this.improvementBo = improvementBo;
        this.taggableCacheManager = taggableCacheManager;
        this.entityCanDoMissionChecker = entityCanDoMissionChecker;
    }

    @Test
    void findUnitTypesWithUserInfo_should_work() {
        var type = givenUnitType();
        type.setSpeedImpactGroup(givenSpeedImpactGroup());
        var typeName = "foo";
        type.setName(typeName);
        var maxCount = 19L;
        type.setMaxCount(maxCount);
        var user = givenUser1();
        user.setFaction(FactionMock.givenFaction());
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

    @SuppressWarnings("ConstantConditions")
    @ParameterizedTest
    @CsvSource({
            "true,true,true",
            "false,true,false",
            "false,false,true",
            "false,false,false"
    })
    void canDoMission_should_work(boolean expected, boolean firstUnitTypeCan, boolean secondUnitTypeCan) {
        var firstUnitType = givenUnitType();
        var secondUnitType = givenUnitType(SECOND_UNIT_TYPE_ID);
        var user = givenUser1();
        var targetPlanet = givenTargetPlanet();
        var missionType = MissionType.EXPLORE;
        given(entityCanDoMissionChecker.canDoMission(user, targetPlanet, firstUnitType, missionType)).willReturn(firstUnitTypeCan);
        given(entityCanDoMissionChecker.canDoMission(user, targetPlanet, secondUnitType, missionType)).willReturn(secondUnitTypeCan);

        assertThat(unitTypeBo.canDoMission(user, targetPlanet, List.of(firstUnitType, secondUnitType), missionType)).isEqualTo(expected);
        verify(entityCanDoMissionChecker, atLeastOnce()).canDoMission(eq(user), eq(targetPlanet), or(eq(firstUnitType), eq(secondUnitType)), eq(missionType));
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(UnitTypeBo.UNIT_TYPE_CACHE_TAG)
                .targetBo(unitTypeBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
