package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.ImageStoreRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = ImageStoreBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImageStoreRepository.class,
        DtoUtilService.class,
        ExceptionUtilService.class,
        TaggableCacheManager.class
})
class ImageStoreBoTest extends AbstractBaseBoTest {
    private final ImageStoreBo imageStoreBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public ImageStoreBoTest(ImageStoreBo imageStoreBo, TaggableCacheManager taggableCacheManager) {
        this.imageStoreBo = imageStoreBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ImageStoreBo.IMAGE_STORE_CACHE_TAG)
                .targetBo(imageStoreBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
