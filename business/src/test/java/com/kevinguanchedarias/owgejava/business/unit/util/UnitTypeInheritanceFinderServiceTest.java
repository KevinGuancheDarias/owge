package com.kevinguanchedarias.owgejava.business.unit.util;

import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitTypeInheritanceFinderService.class
)
@MockBean(UnitTypeRepository.class)
class UnitTypeInheritanceFinderServiceTest {
    private final UnitTypeInheritanceFinderService unitTypeInheritanceFinderService;
    private final UnitTypeRepository unitTypeRepository;

    @Autowired
    public UnitTypeInheritanceFinderServiceTest(
            UnitTypeInheritanceFinderService unitTypeInheritanceFinderService,
            UnitTypeRepository unitTypeRepository
    ) {
        this.unitTypeInheritanceFinderService = unitTypeInheritanceFinderService;
        this.unitTypeRepository = unitTypeRepository;
    }

    @Test
    void findUnitTypeMatchingCondition_should_work_when_argument_matched() {
        var unitType = givenEntity();

        assertThat(unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(unitType, test -> test == unitType))
                .contains(unitType);
    }

    @Test
    void findUnitTypeMatchingCondition_should_work_when_matching_the_parent() {
        var unitType = givenEntity();
        var parent = givenEntity(18);
        unitType.setParent(parent);

        assertThat(unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(unitType, test -> test == parent))
                .contains(parent);
    }

    @Test
    void findUnitTypeMatchingCondition_should_return_empty_when_no_match() {
        var unitType = givenEntity();
        var parent = givenEntity(18);
        unitType.setParent(parent);

        assertThat(unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(unitType, test -> false))
                .isEmpty();
    }

    @Test
    void findUnitTypeMatchingCondition_should_work_when_passing_id() {
        var unitType = givenEntity();
        given(unitTypeRepository.findById(UNIT_TYPE_ID)).willReturn(Optional.of(unitType));

        assertThat(unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(UNIT_TYPE_ID, test -> test == unitType))
                .contains(unitType);

        verify(unitTypeRepository, times(1)).findById(UNIT_TYPE_ID);
    }

    @Test
    void findUnitTypeMatchingCondition_should_throw_when_unit_type_id_is_not_found() {

        assertThatThrownBy(() ->
                unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(UNIT_TYPE_ID, unitType -> true)
        ).isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("No unit type with id " + UNIT_TYPE_ID);

    }
}
