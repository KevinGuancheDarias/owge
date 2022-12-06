package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.business.RequirementGroupBo.REQUIREMENT_GROUP_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.RequirementInformationBo.REQUIREMENT_INFORMATION_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.REFERENCE_ID;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirementInformation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = RequirementInformationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        RequirementInformationDao.class,
        RequirementBo.class,
        RequirementInformationRepository.class,
        TaggableCacheManager.class
})
class RequirementInformationBoTest {
    private final RequirementInformationBo requirementInformationBo;
    private final TaggableCacheManager taggableCacheManager;
    private final RequirementBo requirementBo;
    private final RequirementInformationRepository requirementInformationRepository;

    @Autowired
    RequirementInformationBoTest(RequirementInformationBo requirementInformationBo,
                                 TaggableCacheManager taggableCacheManager,
                                 RequirementBo requirementBo,
                                 RequirementInformationRepository requirementInformationRepository
    ) {
        this.requirementInformationBo = requirementInformationBo;
        this.taggableCacheManager = taggableCacheManager;
        this.requirementBo = requirementBo;
        this.requirementInformationRepository = requirementInformationRepository;
    }

    @Test
    void delete_should_work() {
        var id = 29;
        var requirementInformation = givenRequirementInformation(14);
        requirementInformation.setId(id);
        var or = requirementInformation.getRelation();
        requirementInformation.setRelation(or);
        var deletedRequirement = givenRequirementInformation(28);
        deletedRequirement.setId(id);
        var presentRequirement = givenRequirementInformation(39);
        presentRequirement.setId(9999);
        var mutableRequirements = new ArrayList<>(List.of(deletedRequirement, presentRequirement));
        or.setRequirements(mutableRequirements);
        given(requirementInformationRepository.findById(id)).willReturn(Optional.of(requirementInformation));

        requirementInformationBo.delete(id);

        assertThat(or.getRequirements())
                .hasSize(1)
                .contains(presentRequirement);
        verify(requirementInformationRepository, times(1)).delete(requirementInformation);
        verify(requirementBo, times(1)).triggerRelationChanged(or);
        verify(taggableCacheManager, times(1)).evictByCacheTag(
                REQUIREMENT_INFORMATION_CACHE_TAG,
                ":#UPGRADE_" + REFERENCE_ID
        );
        verify(taggableCacheManager, times(1)).evictByCacheTag(REQUIREMENT_GROUP_CACHE_TAG);
    }
}
