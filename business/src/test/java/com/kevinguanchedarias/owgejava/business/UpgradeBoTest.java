package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.UpgradeRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = UpgradeBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UpgradeRepository.class,
        ObjectRelationBo.class,
        ObtainedUpgradeBo.class,
        ImprovementBo.class,
        TaggableCacheManager.class
})
class UpgradeBoTest extends AbstractBaseBoTest {
    private final UpgradeBo upgradeBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public UpgradeBoTest(UpgradeBo upgradeBo, TaggableCacheManager taggableCacheManager) {
        this.upgradeBo = upgradeBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(UpgradeBo.UPGRADE_CACHE_TAG)
                .targetBo(upgradeBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
