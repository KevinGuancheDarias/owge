package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.TimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.givenTimeSpecial;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = TimeSpecialBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImprovementBo.class,
        ActiveTimeSpecialBo.class,
        UnlockedRelationBo.class,
        UserStorageBo.class,
        TimeSpecialRepository.class,
        TaggableCacheManager.class,
        ObjectRelationsRepository.class,
        UnlockedRelationRepository.class,
        RequirementInformationRepository.class
})
class TimeSpecialBoTest extends AbstractBaseBoTest {
    private final TimeSpecialBo timeSpecialBo;
    private final TaggableCacheManager taggableCacheManager;
    private final ObjectRelationsRepository objectRelationsRepository;
    private final UnlockedRelationRepository unlockedRelationRepository;
    private final TimeSpecialRepository timeSpecialRepository;
    private final ActiveTimeSpecialBo activeTimeSpecialBo;

    @Autowired
    public TimeSpecialBoTest(
            TimeSpecialBo timeSpecialBo,
            TaggableCacheManager taggableCacheManager,
            ObjectRelationsRepository objectRelationsRepository,
            UnlockedRelationRepository unlockedRelationRepository,
            TimeSpecialRepository timeSpecialRepository,
            ActiveTimeSpecialBo activeTimeSpecialBo
    ) {
        this.timeSpecialBo = timeSpecialBo;
        this.taggableCacheManager = taggableCacheManager;
        this.objectRelationsRepository = objectRelationsRepository;
        this.unlockedRelationRepository = unlockedRelationRepository;
        this.timeSpecialRepository = timeSpecialRepository;
        this.activeTimeSpecialBo = activeTimeSpecialBo;
    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(TimeSpecialBo.TIME_SPECIAL_CACHE_TAG)
                .targetBo(timeSpecialBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }

    @Test
    void delete_should_work() {
        var timeSpecial = givenTimeSpecial();
        var or = givenObjectRelation();
        given(timeSpecialRepository.findById(TIME_SPECIAL_ID)).willReturn(Optional.of(timeSpecial));
        given(objectRelationsRepository.findOneByObjectCodeAndReferenceId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(or);

        timeSpecialBo.delete(timeSpecial);
        timeSpecialBo.delete(TIME_SPECIAL_ID);

        verify(activeTimeSpecialBo, times(2)).deleteByTimeSpecial(timeSpecial);
        verify(unlockedRelationRepository, times(2)).deleteByRelation(or);
        verify(objectRelationsRepository, times(2)).delete(or);
        verify(timeSpecialRepository, times(2)).delete(timeSpecial);
    }
}
