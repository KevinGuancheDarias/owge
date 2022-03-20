package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = UnlockedRelationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UnlockedRelationRepository.class,
        ObjectRelationBo.class,
        DtoUtilService.class,
        TaggableCacheManager.class
})
class UnlockedRelationBoTest extends AbstractBaseBoTest {
    private final UnlockedRelationBo unlockedRelationBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    UnlockedRelationBoTest(UnlockedRelationBo unlockedRelationBo, TaggableCacheManager taggableCacheManager) {
        this.unlockedRelationBo = unlockedRelationBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(UnlockedRelationBo.UNLOCKED_RELATION_CACHE_TAG)
                .targetBo(unlockedRelationBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
