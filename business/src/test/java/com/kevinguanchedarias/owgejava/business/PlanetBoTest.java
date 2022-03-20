package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManagerFactory;

@SpringBootTest(
        classes = PlanetBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetRepository.class,
        ExploredPlanetRepository.class,
        UserStorageBo.class,
        ObtainedUnitBo.class,
        MissionBo.class,
        SocketIoService.class,
        RequirementBo.class,
        PlanetListBo.class,
        TaggableCacheManager.class,
        EntityManagerFactory.class
})
class PlanetBoTest extends AbstractBaseBoTest {
    private final PlanetBo planetBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public PlanetBoTest(PlanetBo planetBo, TaggableCacheManager taggableCacheManager) {
        this.planetBo = planetBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(PlanetBo.PLANET_CACHE_TAG)
                .targetBo(planetBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
