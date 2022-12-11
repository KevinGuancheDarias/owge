package com.kevinguanchedarias.owgejava.dao;

import com.kevinguanchedarias.owgejava.business.*;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.entity.RequirementInformation.REQUIREMENT_INFORMATION_CACHE_TAG;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = RequirementInformationDao.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        RequirementInformationRepository.class,
        SpecialLocationBo.class,
        FactionBo.class,
        UpgradeBo.class,
        GalaxyBo.class,
        RequirementRepository.class,
        ObjectRelationBo.class,
        ObjectRelationsRepository.class,
        ExceptionUtilService.class,
        TaggableCacheManager.class
})
class RequirementInformationDaoTest {
    private final RequirementInformationDao requirementInformationDao;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    RequirementInformationDaoTest(RequirementInformationDao requirementInformationDao, TaggableCacheManager taggableCacheManager) {
        this.requirementInformationDao = requirementInformationDao;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Test
    void clearCache_should_work() {
        requirementInformationDao.clearCache();

        verify(taggableCacheManager, times(1)).evictByCacheTag(REQUIREMENT_INFORMATION_CACHE_TAG);
    }
}
