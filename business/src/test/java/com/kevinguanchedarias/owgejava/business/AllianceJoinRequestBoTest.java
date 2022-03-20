package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.AllianceJoinRequestRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = AllianceJoinRequestBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE

)
@MockBean({
        AllianceJoinRequestRepository.class,
        TaggableCacheManager.class
})
class AllianceJoinRequestBoTest extends AbstractBaseBoTest {
    private final AllianceJoinRequestBo allianceJoinRequestBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public AllianceJoinRequestBoTest(AllianceJoinRequestBo allianceJoinRequestBo, TaggableCacheManager taggableCacheManager) {
        this.allianceJoinRequestBo = allianceJoinRequestBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(AllianceJoinRequestBo.ALLIANCE_JOIN_REQUEST_CACHE_TAG)
                .targetBo(allianceJoinRequestBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
