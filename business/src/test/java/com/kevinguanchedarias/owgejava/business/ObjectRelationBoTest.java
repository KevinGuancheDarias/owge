package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.UPGRADE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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

    @Autowired
    ObjectRelationBoTest(
            ObjectRelationBo objectRelationBo,
            ObjectRelationsRepository objectRelationsRepository
    ) {
        this.objectRelationBo = objectRelationBo;
        this.objectRelationsRepository = objectRelationsRepository;
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
}
