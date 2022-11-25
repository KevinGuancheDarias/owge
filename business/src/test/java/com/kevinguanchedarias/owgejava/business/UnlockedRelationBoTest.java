package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenUnlockedRelation;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = UnlockedRelationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UnlockedRelationRepository.class,
        ObjectRelationBo.class,
        DtoUtilService.class
})
class UnlockedRelationBoTest {
    private final UnlockedRelationBo unlockedRelationBo;
    private final UnlockedRelationRepository unlockedRelationRepository;

    @Autowired
    UnlockedRelationBoTest(
            UnlockedRelationBo unlockedRelationBo,
            UnlockedRelationRepository unlockedRelationRepository
    ) {
        this.unlockedRelationBo = unlockedRelationBo;
        this.unlockedRelationRepository = unlockedRelationRepository;
    }

    @Test
    void findByUserIdAndObjectType_should_work() {
        var ur = givenUnlockedRelation(givenUser1());
        given(unlockedRelationRepository.findByUserIdAndRelationObjectCode(USER_ID_1, ObjectEnum.UNIT.name()))
                .willReturn(List.of(ur));

        var result = unlockedRelationBo.findByUserIdAndObjectType(USER_ID_1, ObjectEnum.UNIT);

        Assertions.assertThat(result).hasSize(1).contains(ur);
    }
}
