package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.TimeSpecialRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = TimeSpecialBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImprovementBo.class,
        ActiveTimeSpecialBo.class,
        UnlockedRelationBo.class,
        UserStorageBo.class,
        TimeSpecialRepository.class,
        TaggableCacheManager.class
})
class TimeSpecialBoTest extends AbstractBaseBoTest {
    private final TimeSpecialBo timeSpecialBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public TimeSpecialBoTest(TimeSpecialBo timeSpecialBo, TaggableCacheManager taggableCacheManager) {
        this.timeSpecialBo = timeSpecialBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(TimeSpecialBo.TIME_SPECIAL_CACHE_TAG)
                .targetBo(timeSpecialBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
