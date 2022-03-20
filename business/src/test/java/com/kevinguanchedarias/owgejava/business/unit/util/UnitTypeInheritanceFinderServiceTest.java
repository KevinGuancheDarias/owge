package com.kevinguanchedarias.owgejava.business.unit.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenEntity;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitTypeInheritanceFinderService.class
)
class UnitTypeInheritanceFinderServiceTest {
    private final UnitTypeInheritanceFinderService unitTypeInheritanceFinderService;

    @Autowired
    public UnitTypeInheritanceFinderServiceTest(UnitTypeInheritanceFinderService unitTypeInheritanceFinderService) {
        this.unitTypeInheritanceFinderService = unitTypeInheritanceFinderService;
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
}
