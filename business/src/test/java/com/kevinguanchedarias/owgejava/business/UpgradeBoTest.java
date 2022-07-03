package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.UpgradeRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenImprovement;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUpgradeMock.givenObtainedUpgrade;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.UPGRADE_ID;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.givenUpgrade;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = UpgradeBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UpgradeRepository.class,
        ObjectRelationBo.class,
        ObtainedUpgradeBo.class,
        ImprovementBo.class,
        TaggableCacheManager.class
})
class UpgradeBoTest extends AbstractBaseBoTest {
    private final UpgradeBo upgradeBo;
    private final TaggableCacheManager taggableCacheManager;
    private final ImprovementBo improvementBo;
    private final ObjectRelationBo objectRelationBo;
    private final ObtainedUpgradeBo obtainedUpgradeBo;
    private final UpgradeRepository upgradeRepository;

    @Autowired
    public UpgradeBoTest(
            UpgradeBo upgradeBo,
            TaggableCacheManager taggableCacheManager,
            ImprovementBo improvementBo,
            ObjectRelationBo objectRelationBo,
            ObtainedUpgradeBo obtainedUpgradeBo,
            UpgradeRepository upgradeRepository
    ) {
        this.upgradeBo = upgradeBo;
        this.taggableCacheManager = taggableCacheManager;
        this.improvementBo = improvementBo;
        this.objectRelationBo = objectRelationBo;
        this.obtainedUpgradeBo = obtainedUpgradeBo;
        this.upgradeRepository = upgradeRepository;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(UpgradeBo.UPGRADE_CACHE_TAG)
                .targetBo(upgradeBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }

    @ParameterizedTest
    @MethodSource("delete_should_work_arguments")
    void delete_should_work(Improvement upgradeImprovement, int expectedCalls) {
        var upgrade = givenUpgrade();
        upgrade.setImprovement(upgradeImprovement);
        var ou = givenObtainedUpgrade();
        var user = ou.getUser();
        var or = givenObjectRelation();
        given(upgradeRepository.findById(UPGRADE_ID)).willReturn(Optional.of(upgrade));
        given(objectRelationBo.findOneOpt(ObjectEnum.UPGRADE, UPGRADE_ID)).willReturn(Optional.of(or));
        given(obtainedUpgradeBo.findByUpgrade(upgrade)).willReturn(List.of(ou));

        upgradeBo.delete(UPGRADE_ID);

        verify(improvementBo, times(1)).clearCacheEntriesIfRequired(eq(upgrade), any(ObtainedUpgradeBo.class));
        verify(objectRelationBo, times(1)).delete(or);
        verify(obtainedUpgradeBo, times(1)).deleteByUpgrade(upgrade);
        verify(obtainedUpgradeBo, times(1)).emitObtainedChange(USER_ID_1);
        verify(upgradeRepository, times(1)).delete(upgrade);
        verify(improvementBo, times(expectedCalls)).emitUserImprovement(user);

    }

    private static Stream<Arguments> delete_should_work_arguments() {
        return Stream.of(
                Arguments.of(givenImprovement(), 1),
                Arguments.of(null, 0)
        );
    }

}
