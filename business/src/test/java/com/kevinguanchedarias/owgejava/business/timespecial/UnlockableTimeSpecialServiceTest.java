package com.kevinguanchedarias.owgejava.business.timespecial;

import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = UnlockableTimeSpecialService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(UnlockedRelationBo.class)
class UnlockableTimeSpecialServiceTest {
    private final UnlockableTimeSpecialService unlockableTimeSpecialService;
    private final UnlockedRelationBo unlockedRelationBo;

    @Autowired
    UnlockableTimeSpecialServiceTest(UnlockableTimeSpecialService unlockableTimeSpecialService, UnlockedRelationBo unlockedRelationBo) {
        this.unlockableTimeSpecialService = unlockableTimeSpecialService;
        this.unlockedRelationBo = unlockedRelationBo;
    }

    @Test
    void getDtoClass_should_work() {
        assertThat(unlockableTimeSpecialService.getDtoClass()).isEqualTo(TimeSpecialDto.class);
    }

    @Test
    void getObject_should_work() {
        assertThat(unlockableTimeSpecialService.getObject()).isEqualTo(ObjectEnum.TIME_SPECIAL);
    }

    @Test
    void getUnlockedRelationBo_should_work() {
        assertThat(unlockableTimeSpecialService.getUnlockedRelationBo()).isSameAs(unlockedRelationBo);
    }
}
