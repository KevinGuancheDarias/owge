package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendTargetNotUnlocked;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.*;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.UPGRADE_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = ObjectRelationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObjectEntityBo.class,
        UnlockedRelationRepository.class,
        ObjectRelationsRepository.class,
        RequirementInformationRepository.class,
})
class ObjectRelationBoTest {
    private final ObjectRelationBo objectRelationBo;
    private final ObjectRelationsRepository objectRelationsRepository;
    private final UnlockedRelationRepository unlockedRelationRepository;
    private final RequirementInformationRepository requirementInformationRepository;

    @Autowired
    ObjectRelationBoTest(
            ObjectRelationBo objectRelationBo,
            ObjectRelationsRepository objectRelationsRepository,
            UnlockedRelationRepository unlockedRelationRepository,
            RequirementInformationRepository requirementInformationRepository
    ) {
        this.objectRelationBo = objectRelationBo;
        this.objectRelationsRepository = objectRelationsRepository;
        this.unlockedRelationRepository = unlockedRelationRepository;
        this.requirementInformationRepository = requirementInformationRepository;
    }

    @Test
    void findOne_should_work() {
        var or = givenObjectRelation();
        given(objectRelationsRepository.findOneByObjectCodeAndReferenceId(ObjectEnum.UPGRADE.name(), UPGRADE_ID))
                .willReturn(or);

        var result = objectRelationBo.findOne(ObjectEnum.UPGRADE, UPGRADE_ID);

        assertThat(result).isSameAs(or);
    }

    @Test
    void findOneOpt_should_work() {
        var or = givenObjectRelation();
        given(objectRelationsRepository.findOneByObjectCodeAndReferenceId(ObjectEnum.UPGRADE.name(), UPGRADE_ID))
                .willReturn(or);

        var result = objectRelationBo.findOneOpt(ObjectEnum.UPGRADE, UPGRADE_ID);

        assertThat(result).contains(or);
    }

    @Test
    void checkIsUnlocked_should_throw_when_not_unlocked() {
        assertThatThrownBy(() -> objectRelationBo.checkIsUnlocked(USER_ID_1, OBJECT_RELATION_ID))
                .isInstanceOf(SgtBackendTargetNotUnlocked.class);
    }

    @Test
    void checkIsUnlocked_should_not_throw() {
        var user = givenUser1();
        var or = givenObjectRelation();
        var ur = givenUnlockedRelation(user);
        given(unlockedRelationRepository.findOneByUserIdAndRelationId(USER_ID_1, OBJECT_RELATION_ID)).willReturn(ur);

        assertThatNoException().isThrownBy(() -> objectRelationBo.checkIsUnlocked(user, or));
    }

    @Test
    void delete_should_work() {
        var or = givenObjectRelation();

        objectRelationBo.delete(or);

        verify(requirementInformationRepository, times(1)).deleteByRelation(or);
        verify(unlockedRelationRepository, times(1)).deleteByRelation(or);
        verify(objectRelationsRepository, times(1)).delete(or);
    }
}
