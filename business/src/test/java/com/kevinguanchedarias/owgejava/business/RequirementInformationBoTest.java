package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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
class RequirementInformationBoTest extends AbstractBaseBoTest {
    private final RequirementInformationBo requirementInformationBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    RequirementInformationBoTest(RequirementInformationBo requirementInformationBo, TaggableCacheManager taggableCacheManager) {
        this.requirementInformationBo = requirementInformationBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(RequirementInformationBo.REQUIREMENT_INFORMATION_CACHE_TAG)
                .targetBo(requirementInformationBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
