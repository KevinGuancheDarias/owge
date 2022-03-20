package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = ObtainedUpgradeBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUpgradeRepository.class,
        ImprovementBo.class,
        SocketIoService.class,
        TaggableCacheManager.class
})
class ObtainedUpgradeBoTest extends AbstractBaseBoTest {
    private final ObtainedUpgradeBo obtainedUpgradeBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public ObtainedUpgradeBoTest(ObtainedUpgradeBo obtainedUpgradeBo, TaggableCacheManager taggableCacheManager) {
        this.obtainedUpgradeBo = obtainedUpgradeBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ObtainedUpgradeBo.OBTAINED_UPGRADE_CACHE_TAG)
                .targetBo(obtainedUpgradeBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
