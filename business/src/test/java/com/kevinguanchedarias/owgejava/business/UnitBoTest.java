package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.InterceptableSpeedGroupRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = UnitBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UnitRepository.class,
        UnlockedRelationBo.class,
        InterceptableSpeedGroupRepository.class,
        SpeedImpactGroupBo.class,
        CriticalAttackBo.class,
        ObtainedUnitRepository.class,
        TaggableCacheManager.class
})
class UnitBoTest extends AbstractBaseBoTest {
    private final UnitBo unitBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    UnitBoTest(UnitBo unitBo, TaggableCacheManager taggableCacheManager) {
        this.unitBo = unitBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(UnitBo.UNIT_CACHE_TAG)
                .targetBo(unitBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
