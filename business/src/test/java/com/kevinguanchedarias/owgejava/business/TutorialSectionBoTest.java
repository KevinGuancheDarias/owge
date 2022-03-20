package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.TutorialSectionAvailableHtmlSymbolRepository;
import com.kevinguanchedarias.owgejava.repository.TutorialSectionEntryRepository;
import com.kevinguanchedarias.owgejava.repository.TutorialSectionRepository;
import com.kevinguanchedarias.owgejava.repository.VisitedTutorialSectionEntryRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = TutorialSectionBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        TutorialSectionRepository.class,
        TutorialSectionEntryRepository.class,
        TutorialSectionAvailableHtmlSymbolRepository.class,
        VisitedTutorialSectionEntryRepository.class,
        UserStorageBo.class,
        TranslationBo.class,
        SocketIoService.class,
        DtoUtilService.class,
        TaggableCacheManager.class
})
class TutorialSectionBoTest extends AbstractBaseBoTest {
    private final TutorialSectionBo tutorialSectionBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    TutorialSectionBoTest(TutorialSectionBo tutorialSectionBo, TaggableCacheManager taggableCacheManager) {
        this.tutorialSectionBo = tutorialSectionBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(TutorialSectionBo.TUTORIAL_SECTION_CACHE_TAG)
                .targetBo(tutorialSectionBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
