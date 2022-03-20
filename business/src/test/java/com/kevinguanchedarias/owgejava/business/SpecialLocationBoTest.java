package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.SpecialLocationRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = SpecialLocationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        SpecialLocationRepository.class,
        PlanetBo.class,
        TaggableCacheManager.class
})
class SpecialLocationBoTest extends AbstractBaseBoTest {
    private final SpecialLocationBo specialLocationBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public SpecialLocationBoTest(SpecialLocationBo specialLocationBo, TaggableCacheManager taggableCacheManager) {
        this.specialLocationBo = specialLocationBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(SpecialLocationBo.SPECIAL_LOCATION_CACHE_TAG)
                .targetBo(specialLocationBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
