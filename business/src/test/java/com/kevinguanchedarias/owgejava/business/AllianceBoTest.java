package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.AllianceRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.kevinguanchedarias.owgejava.mock.AllianceMock.givenAlliance;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = AllianceBo.class
)
@MockBean({
        AllianceRepository.class,
        UserStorageBo.class,
        AllianceJoinRequestBo.class,
        ConfigurationBo.class,
        AuditBo.class,
        TaggableCacheManager.class
})
class AllianceBoTest extends AbstractBaseBoTest {
    private final AllianceBo allianceBo;
    private final TaggableCacheManager taggableCacheManager;

    @Autowired
    AllianceBoTest(AllianceBo allianceBo, TaggableCacheManager taggableCacheManager) {
        this.allianceBo = allianceBo;
        this.taggableCacheManager = taggableCacheManager;
    }

    @Test
    void areEnemies_should_be_true_when_source_has_no_alliance() {
        var sourceUser = givenUser1();
        var targetUser = givenUser2();
        targetUser.setAlliance(givenAlliance());

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isTrue();
    }

    @Test
    void areEnemies_should_be_true_when_target_has_no_alliance() {
        var sourceUser = givenUser1();
        var targetUser = givenUser2();
        sourceUser.setAlliance(givenAlliance());

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isTrue();
    }

    @Test
    void areEnemies_should_be_true_when_both_has_different_non_null_alliances() {
        var sourceUser = givenUser1();
        var targetUser = givenUser2();
        sourceUser.setAlliance(givenAlliance(12));
        targetUser.setAlliance(givenAlliance());

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isTrue();
    }

    @Test
    void areEnemies_should_be_false_when_both_users_has_same_alliance() {
        var sourceUser = givenUser1();
        var targetUser = givenUser2();
        sourceUser.setAlliance(givenAlliance());
        targetUser.setAlliance(givenAlliance());

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isFalse();
    }

    @Test
    void areEnemies_should_handle_userIds() {
        var sourceUser = givenUser1();
        var targetUser = givenUser1();

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isFalse();
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(AllianceBo.ALLIANCE_CACHE_TAG)
                .taggableCacheManager(taggableCacheManager)
                .targetBo(allianceBo)
                .build();
    }
}
