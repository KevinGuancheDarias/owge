package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.EntityWithRequirementGroups;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.OBJECT_RELATION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelationToObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.RequirementGroupMock.givenRequirementGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
    private final ObjectRelationToObjectRelationRepository objectRelationToObjectRelationRepository;
    private final ObjectRelationBo objectRelationBo;

    @Autowired
    public RequirementGroupBoTest(
            RequirementGroupBo requirementGroupBo,
            TaggableCacheManager taggableCacheManager,
            ObjectRelationToObjectRelationRepository objectRelationToObjectRelationRepository,
            ObjectRelationBo objectRelationBo
    ) {
        this.requirementGroupBo = requirementGroupBo;
        this.taggableCacheManager = taggableCacheManager;
        this.objectRelationToObjectRelationRepository = objectRelationToObjectRelationRepository;
        this.objectRelationBo = objectRelationBo;
    }


    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(RequirementGroupBo.REQUIREMENT_GROUP_CACHE_TAG)
                .targetBo(requirementGroupBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }

    @Test
    void findRequirements_should_work() {
        var intermediateRelation = givenObjectRelationToObjectRelation();
        var entityWithRequirements = mock(EntityWithRequirementGroups.class);
        var requirementGroup = givenRequirementGroup();
        var or = givenObjectRelation();
        given(entityWithRequirements.getRelation()).willReturn(or);
        given(objectRelationToObjectRelationRepository.findByMasterId(OBJECT_RELATION_ID)).willReturn(List.of(intermediateRelation));
        given(objectRelationBo.unboxObjectRelation(intermediateRelation.getSlave())).willReturn(requirementGroup);

        assertThat(requirementGroupBo.findRequirements(entityWithRequirements))
                .isNotEmpty()
                .contains(requirementGroup);

    }

}
