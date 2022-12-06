package com.kevinguanchedarias.owgejava.business.rule.itemtype;

import com.kevinguanchedarias.owgejava.dto.base.IdNameDto;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.UnitMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitRuleItemTypeProviderBo.class
)
@MockBean(UnitRepository.class)
class UnitRuleItemTypeProviderBoTest {
    private final UnitRuleItemTypeProviderBo unitRuleItemTypeProviderBo;
    private final UnitRepository unitRepository;

    @Autowired
    UnitRuleItemTypeProviderBoTest(UnitRuleItemTypeProviderBo unitRuleItemTypeProviderBo, UnitRepository unitRepository) {
        this.unitRuleItemTypeProviderBo = unitRuleItemTypeProviderBo;
        this.unitRepository = unitRepository;
    }

    @Test
    void getRuleItemTypeId_should_work() {
        assertThat(this.unitRuleItemTypeProviderBo.getRuleItemTypeId()).isEqualTo(UnitRuleItemTypeProviderBo.PROVIDER_ID);
    }

    @Test
    void findRuleItemTypeDescriptor_should_work() {
        var item = givenUnit1();
        when(unitRepository.findAll()).thenReturn(List.of(item));
        var expectedResult = IdNameDto.builder().id(UNIT_ID_1).name(UNIT_NAME).build();

        var result = unitRuleItemTypeProviderBo.findRuleItemTypeDescriptor();

        verify(unitRepository, times(1)).findAll();
        assertThat(result.getItems()).hasSize(1).contains(expectedResult);
    }
}
