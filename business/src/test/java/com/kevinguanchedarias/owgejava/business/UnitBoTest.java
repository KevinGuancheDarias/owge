package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.InterceptableSpeedGroupRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenUnlockedRelation;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest(
        classes = UnitBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UnlockedRelationBo.class,
        UnitRepository.class,
        InterceptableSpeedGroupRepository.class,
        SpeedImpactGroupBo.class,
        CriticalAttackBo.class,
        ObtainedUnitRepository.class,
        EntityManager.class,
        HiddenUnitBo.class,
        SpeedImpactGroupBo.class,
        SpeedImpactGroupFinderBo.class
})
class UnitBoTest {
    private final UnitBo unitBo;
    private final UnlockedRelationBo unlockedRelationBo;
    private final EntityManager entityManager;
    private final HiddenUnitBo hiddenUnitBo;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;

    @Autowired
    UnitBoTest(
            UnitBo unitBo,
            UnlockedRelationBo unlockedRelationBo,
            EntityManager entityManager,
            HiddenUnitBo hiddenUnitBo,
            SpeedImpactGroupFinderBo speedImpactGroupFinderBo
    ) {
        this.unitBo = unitBo;
        this.unlockedRelationBo = unlockedRelationBo;
        this.entityManager = entityManager;
        this.hiddenUnitBo = hiddenUnitBo;
        this.speedImpactGroupFinderBo = speedImpactGroupFinderBo;
    }

    @ParameterizedTest
    @MethodSource("findAllByUser_should_work_arguments")
    void findAllByUser_should_work(
            boolean isHiddenUnit,
            SpeedImpactGroup unitSpeedImpactGroup,
            int timesFindApplicable
    ) {
        var user = givenUser1();
        var ur = List.of(givenUnlockedRelation(user));
        var unit = givenUnit1();
        unit.setSpeedImpactGroup(unitSpeedImpactGroup);
        var spi = givenSpeedImpactGroup();
        var unitDto = new UnitDto();
        unitDto.dtoFromEntity(unit);
        spi.setRequirementGroups(List.of(new RequirementGroup()));
        given(unlockedRelationBo.findByUserIdAndObjectType(USER_ID_1, ObjectEnum.UNIT)).willReturn(ur);
        given(unlockedRelationBo.unboxToTargetEntity(ur)).willReturn(List.of(unit));
        given(hiddenUnitBo.isHiddenUnit(user, unit)).willReturn(isHiddenUnit);
        given(speedImpactGroupFinderBo.findApplicable(user, unit)).willReturn(spi);

        try (var dtoUtil = mockStatic(DtoUtilService.class)) {
            var captor = ArgumentCaptor.forClass(Unit.class);
            dtoUtil.when(() -> DtoUtilService.staticDtoFromEntity(eq(UnitDto.class), captor.capture()))
                    .thenReturn(unitDto);

            var result = unitBo.findAllByUser(user);

            var passedEntity = captor.getValue();
            assertThat(passedEntity.getSpeedImpactGroup().getRequirementGroups()).isNull();
            assertThat(passedEntity.getIsInvisible()).isEqualTo(isHiddenUnit);
            assertThat(result).isNotEmpty();
            var unitDtoResult = result.get(0);
            assertThat(unitDtoResult.getId()).isEqualTo(UNIT_ID_1);
        }
    }

    private static Stream<Arguments> findAllByUser_should_work_arguments() {
        var spi = givenSpeedImpactGroup();
        spi.setRequirementGroups(List.of(new RequirementGroup()));
        return Stream.of(
                Arguments.of(true, null, 1),
                Arguments.of(false, spi, 0)
        );
    }
}
