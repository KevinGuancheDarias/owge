package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructAdminUserBo;
import com.kevinguanchedarias.owgejava.repository.AdminUserRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        classes = NonPostConstructAdminUserBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        AdminUserRepository.class,
        AuthenticationBo.class,
        ConfigurationBo.class,
        JwtService.class,
        TaggableCacheManager.class,
})
public class AdminUserBoTest extends AbstractBaseBoTest {
    private final AdminUserBo adminUserBo;
    private final TaggableCacheManager taggableCacheManager;

    @MockBean(name = "adminOwgeTokenConfigLoader")
    private TokenConfigLoader tokenConfigLoader;

    @Autowired
    public AdminUserBoTest(AdminUserBo adminUserBo, TaggableCacheManager taggableCacheManager) {
        this.adminUserBo = adminUserBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(AdminUserBo.ADMIN_USER_CACHE_TAG)
                .targetBo(adminUserBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
