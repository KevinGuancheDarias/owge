package com.kevinguanchedarias.owgejava.business;


import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = ObjectRelationToObjectRelationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObjectRelationToObjectRelationRepository.class,
        TaggableCacheManager.class
})
class ObjectRelationToObjectRelationBoTest extends AbstractBaseBoTest {
    private final ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public ObjectRelationToObjectRelationBoTest(ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo, TaggableCacheManager taggableCacheManager) {
        this.objectRelationToObjectRelationBo = objectRelationToObjectRelationBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ObjectRelationToObjectRelationBo.OBJECT_RELATION_TO_OBJECT_RELATION_CACHE_TAG)
                .targetBo(objectRelationToObjectRelationBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
