package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;

@SpringBootTest(
        classes = UserStorageBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UserStorageRepository.class,
        FactionBo.class,
        PlanetBo.class,
        RequirementBo.class,
        ObtainedUnitBo.class,
        AllianceBo.class,
        AuthenticationBo.class,
        ImprovementBo.class,
        EntityManager.class,
        SocketIoService.class,
        DtoUtilService.class,
        FactionSpawnLocationBo.class,
        TaggableCacheManager.class,
        AuditBo.class
})
public class UserStorageBoTest extends AbstractBaseBoTest {
    private final UserStorageBo userStorageBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    public UserStorageBoTest(UserStorageBo userStorageBo, TaggableCacheManager taggableCacheManager) {
        this.userStorageBo = userStorageBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(UserStorageBo.USER_CACHE_TAG)
                .targetBo(userStorageBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
