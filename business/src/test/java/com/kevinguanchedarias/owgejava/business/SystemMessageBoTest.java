package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.repository.SystemMessageRepository;
import com.kevinguanchedarias.owgejava.repository.UserReadSystemMessageRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = SystemMessageBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        SystemMessageRepository.class,
        TransactionUtilService.class,
        SocketIoService.class,
        UserStorageBo.class,
        UserReadSystemMessageRepository.class,
        TaggableCacheManager.class
})
class SystemMessageBoTest extends AbstractBaseBoTest {
    private final SystemMessageBo systemMessageBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    SystemMessageBoTest(SystemMessageBo systemMessageBo, TaggableCacheManager taggableCacheManager) {
        this.systemMessageBo = systemMessageBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(SystemMessageBo.SYSTEM_MESSAGE_CACHE_TAG)
                .targetBo(systemMessageBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
