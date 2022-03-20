package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.ImprovementRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

@SpringBootTest(
        classes = ImprovementBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImprovementRepository.class,
        DtoUtilService.class,
        CacheManager.class,
        ConfigurationBo.class,
        SocketIoService.class,
        TaggableCacheManager.class
})
class ImprovementBoTest extends AbstractBaseBoTest {
    private final ImprovementBo improvementBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public ImprovementBoTest(ImprovementBo improvementBo, TaggableCacheManager taggableCacheManager) {
        this.improvementBo = improvementBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ImprovementBo.IMPROVEMENT_CACHE_TAG)
                .targetBo(improvementBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
