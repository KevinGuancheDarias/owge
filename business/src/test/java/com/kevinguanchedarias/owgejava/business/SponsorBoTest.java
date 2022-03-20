package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.SponsorRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = SponsorBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        SponsorRepository.class,
        TaggableCacheManager.class
})
class SponsorBoTest extends AbstractBaseBoTest {
    private final SponsorBo sponsorBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    SponsorBoTest(SponsorBo sponsorBo, TaggableCacheManager taggableCacheManager) {
        this.sponsorBo = sponsorBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(SponsorBo.SPONSOR_CACHE_TAG)
                .targetBo(sponsorBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
