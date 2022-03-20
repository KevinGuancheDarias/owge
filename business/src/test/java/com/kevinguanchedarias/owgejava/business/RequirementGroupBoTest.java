package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = RequirementGroupBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        RequirementGroupRepository.class,
        ObjectRelationBo.class,
        RequirementBo.class,
        ObjectRelationToObjectRelationRepository.class,
        TaggableCacheManager.class
})
class RequirementGroupBoTest extends AbstractBaseBoTest {
    private final RequirementGroupBo requirementGroupBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public RequirementGroupBoTest(RequirementGroupBo requirementGroupBo, TaggableCacheManager taggableCacheManager) {
        this.requirementGroupBo = requirementGroupBo;
        this.taggableCacheManager = taggableCacheManager;
    }


    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(RequirementGroupBo.REQUIREMENT_GROUP_CACHE_TAG)
                .targetBo(requirementGroupBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
