package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.UPGRADE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = ObjectRelationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObjectEntityBo.class,
        UnlockedRelationBo.class,
        ObjectRelationsRepository.class,
        RequirementInformationBo.class,
        TaggableCacheManager.class,
})
class ObjectRelationBoTest extends AbstractBaseBoTest {
    private final ObjectRelationBo objectRelationBo;
    private final TaggableCacheManager taggableCacheManager;
    private final ObjectRelationsRepository objectRelationsRepository;

    @Autowired
    ObjectRelationBoTest(
            ObjectRelationBo objectRelationBo,
            TaggableCacheManager taggableCacheManager,
            ObjectRelationsRepository objectRelationsRepository
    ) {
        this.objectRelationBo = objectRelationBo;
        this.taggableCacheManager = taggableCacheManager;
        this.objectRelationsRepository = objectRelationsRepository;
    }


    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ObjectRelationBo.OBJECT_RELATION_CACHE_TAG)
                .targetBo(objectRelationBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }

    @Test
    void findOne_should_work() {
        var or = givenObjectRelation();
        given(objectRelationsRepository.findOneByObjectCodeAndReferenceId(ObjectEnum.UPGRADE.name(), UPGRADE_ID))
                .willReturn(or);

        var result = objectRelationBo.findOne(ObjectEnum.UPGRADE, UPGRADE_ID);

        assertThat(result).isSameAs(or);
    }
}
