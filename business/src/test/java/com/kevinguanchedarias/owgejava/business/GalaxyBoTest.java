package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = GalaxyBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        GalaxyRepository.class,
        PlanetRepository.class,
        PlanetBo.class,
        TaggableCacheManager.class
})
class GalaxyBoTest extends AbstractBaseBoTest {
    private final GalaxyBo galaxyBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public GalaxyBoTest(GalaxyBo galaxyBo, TaggableCacheManager taggableCacheManager) {
        this.galaxyBo = galaxyBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(GalaxyBo.GALAXY_CACHE_TAG)
                .targetBo(galaxyBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
