package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = ObjectRelationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObjectEntityBo.class,
        UnlockedRelationBo.class,
        ObjectRelationsRepository.class,
        RequirementInformationBo.class,
        TaggableCacheManager.class
})
class ObjectRelationBoTest extends AbstractBaseBoTest {
    private final ObjectRelationBo objectRelationBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    ObjectRelationBoTest(ObjectRelationBo objectRelationBo, TaggableCacheManager taggableCacheManager) {
        this.objectRelationBo = objectRelationBo;
        this.taggableCacheManager = taggableCacheManager;
    }


    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ObjectRelationBo.OBJECT_RELATION_CACHE_TAG)
                .targetBo(objectRelationBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
