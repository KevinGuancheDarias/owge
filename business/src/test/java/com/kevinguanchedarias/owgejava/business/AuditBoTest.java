package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.AuditRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = AuditBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        AuditRepository.class,
        UserStorageBo.class,
        TorClientBo.class,
        AsyncRunnerBo.class,
        SocketIoService.class,
        TaggableCacheManager.class
})
class AuditBoTest extends AbstractBaseBoTest {
    private final AuditBo auditBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public AuditBoTest(AuditBo auditBo, TaggableCacheManager taggableCacheManager) {
        this.auditBo = auditBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(AuditBo.AUDIT_CACHE_TAG)
                .targetBo(auditBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
