package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.UpgradeTypeRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = UpgradeTypeBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UpgradeTypeRepository.class,
        TaggableCacheManager.class
})
public class UpgradeTypeBoTest extends AbstractBaseBoTest {
    private final UpgradeTypeBo upgradeTypeBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public UpgradeTypeBoTest(UpgradeTypeBo upgradeTypeBo, TaggableCacheManager taggableCacheManager) {
        this.upgradeTypeBo = upgradeTypeBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(UpgradeTypeBo.UPGRADE_TYPE_CACHE_TAG)
                .targetBo(upgradeTypeBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
