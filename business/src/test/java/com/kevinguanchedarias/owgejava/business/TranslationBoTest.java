package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.TranslatableRepository;
import com.kevinguanchedarias.owgejava.repository.TranslatableTranslationRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = TranslationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        TranslatableRepository.class,
        TranslatableTranslationRepository.class,
        DtoUtilService.class,
        TaggableCacheManager.class
})
class TranslationBoTest extends AbstractBaseBoTest {
    private final TranslationBo translationBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    TranslationBoTest(TranslationBo translationBo, TaggableCacheManager taggableCacheManager) {
        this.translationBo = translationBo;
        this.taggableCacheManager = taggableCacheManager;
    }


    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(TranslationBo.TRANSLATION_CACHE_TAG)
                .targetBo(translationBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
