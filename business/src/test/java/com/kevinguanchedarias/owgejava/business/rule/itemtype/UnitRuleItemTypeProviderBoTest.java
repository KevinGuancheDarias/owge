package com.kevinguanchedarias.owgejava.business.rule.itemtype;

import com.kevinguanchedarias.owgejava.business.UnitBo;
import com.kevinguanchedarias.owgejava.dto.base.IdNameDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_NAME;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitRuleItemTypeProviderBo.class
)
@MockBean(UnitBo.class)
class UnitRuleItemTypeProviderBoTest {
    private final UnitRuleItemTypeProviderBo unitRuleItemTypeProviderBo;
    private final UnitBo unitBo;

    @Autowired
    UnitRuleItemTypeProviderBoTest(UnitRuleItemTypeProviderBo unitRuleItemTypeProviderBo, UnitBo unitBo) {
        this.unitRuleItemTypeProviderBo = unitRuleItemTypeProviderBo;
        this.unitBo = unitBo;
    }

    @Test
    void getRuleItemTypeId_should_work() {
        assertThat(this.unitRuleItemTypeProviderBo.getRuleItemTypeId()).isEqualTo(UnitRuleItemTypeProviderBo.PROVIDER_ID);
    }

    @Test
    void findRuleItemTypeDescriptor_should_work() {
        var item = givenUnit1();
        when(unitBo.findAll()).thenReturn(List.of(item));
        var expectedResult = IdNameDto.builder().id(UNIT_ID_1).name(UNIT_NAME).build();

        var result = unitRuleItemTypeProviderBo.findRuleItemTypeDescriptor();

        verify(unitBo, times(1)).findAll();
        assertThat(result.getItems()).hasSize(1).contains(expectedResult);
    }
}
