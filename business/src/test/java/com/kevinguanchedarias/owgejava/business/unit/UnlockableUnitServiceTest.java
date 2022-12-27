package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = UnlockableUnitService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(UnlockedRelationBo.class)
class UnlockableUnitServiceTest {
    private final UnlockableUnitService unlockableUnitService;
    private final UnlockedRelationBo unlockedRelationBo;

    @Autowired
    UnlockableUnitServiceTest(UnlockableUnitService unlockableUnitService, UnlockedRelationBo unlockedRelationBo) {
        this.unlockableUnitService = unlockableUnitService;
        this.unlockedRelationBo = unlockedRelationBo;
    }

    @Test
    void getDtoClass_should_work() {
        assertThat(unlockableUnitService.getDtoClass()).isEqualTo(UnitDto.class);
    }

    @Test
    void getObject_should_work() {
        assertThat(unlockableUnitService.getObject()).isEqualTo(ObjectEnum.UNIT);
    }

    @Test
    void getUnlockedRelationBo_should_work() {
        assertThat(unlockableUnitService.getUnlockedRelationBo()).isSameAs(unlockedRelationBo);
    }
}
