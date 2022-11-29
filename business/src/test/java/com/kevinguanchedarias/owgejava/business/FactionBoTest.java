package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.converter.faction.FactionSpawnLocationDtoToEntityConverter;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.entity.FactionUnitType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.fake.ImprovementHibernateProxy;
import com.kevinguanchedarias.owgejava.mock.UnitTypeMock;
import com.kevinguanchedarias.owgejava.repository.FactionRepository;
import com.kevinguanchedarias.owgejava.repository.FactionSpawnLocationRepository;
import com.kevinguanchedarias.owgejava.repository.FactionUnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collection;
import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.FactionMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                FactionBo.class,
                FactionSpawnLocationDtoToEntityConverter.class,
                DefaultConversionService.class
        }
)
@MockBean({
        FactionRepository.class,
        FactionUnitTypeRepository.class,
        UnitTypeBo.class,
        FactionSpawnLocationRepository.class,
        GalaxyRepository.class
})
class FactionBoTest {

    private final FactionBo factionBo;
    private final FactionRepository factionRepository;
    private final UnitTypeBo unitTypeBo;
    private final FactionUnitTypeRepository factionUnitTypeRepository;

    @Autowired
    public FactionBoTest(
            FactionBo factionBo,
            FactionRepository factionRepository,
            UnitTypeBo unitTypeBo,
            FactionUnitTypeRepository factionUnitTypeRepository,
            DefaultConversionService conversionService,
            Collection<Converter<?, ?>> converters
    ) {
        this.factionBo = factionBo;
        this.factionRepository = factionRepository;
        this.unitTypeBo = unitTypeBo;
        this.factionUnitTypeRepository = factionUnitTypeRepository;
        converters.forEach(conversionService::addConverter);
    }

    @Test
    void getRepository_should_return_repository() {
        assertEquals(factionRepository, this.factionBo.getRepository());
    }

    @Test
    void getDtoClass_should_work() {
        assertEquals(FactionDto.class, this.factionBo.getDtoClass());
    }

    @Test
    void findVisible_should_not_lazy_load_faction_improvements() {
        var expected = givenFaction();
        given(factionRepository.findByHiddenFalse()).willReturn(List.of(givenFaction()));

        var result = factionBo.findVisible(false);

        assertThat(result).hasSize(1);
        var faction = result.get(0);
        assertEquals(expected.getId(), faction.getId());
        assertNull(faction.getImprovement());
    }

    @Test
    void findVisible_should_lazy_load_faction_improvements() {
        var expected = givenFaction();
        expected.setImprovement(new ImprovementHibernateProxy());
        given(factionRepository.findByHiddenFalse()).willReturn(List.of(givenFaction()));

        var result = factionBo.findVisible(true);

        assertThat(result).hasSize(1);
        var faction = result.get(0);
        assertEquals(expected.getId(), faction.getId());
        assertNotNull(faction.getImprovement());
    }

    @Test
    void save_should_throw_when_custom_resource_gather_percentages_are_greather_than_100() {
        var expected = givenFaction();
        expected.setCustomPrimaryGatherPercentage(90F);
        expected.setCustomSecondaryGatherPercentage(11F);
        assertThatThrownBy(() -> factionBo.save(expected))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("custom primary percentage plus secondary");
    }

    @Test
    void save_should_work() {
        var givenEntity = givenFaction();
        given(factionRepository.save(givenEntity)).willReturn(givenEntity);

        var result = factionBo.save(givenEntity);

        verify(factionRepository, times(1)).save(givenEntity);
        assertEquals(1, result.getId());
    }

    @Test
    void findByUser_should_work() {
        var userId = 1;
        var givenEntity = givenFaction();
        given(factionRepository.findOneByUsersId(userId)).willReturn(givenEntity);

        var result = factionBo.findByUser(userId);

        verify(factionRepository, times(1)).findOneByUsersId(userId);
        assertEquals(1, result.getId());
    }

    @Test
    void saveOverrides_should_delete_old_overrides_and_save_new_ones() {
        var factionId = 1;
        var givenFaction = givenFaction();
        var unitType = UnitTypeMock.givenUnitType(UNIT_TYPE_ID);
        given(unitTypeBo.getOne(UNIT_TYPE_ID)).willReturn(unitType);
        given(factionRepository.getById(factionId)).willReturn(givenFaction);

        this.factionBo.saveOverrides(factionId, List.of(givenOverride()));

        verify(factionUnitTypeRepository, times(1)).deleteByFactionId(factionId);
        verify(unitTypeBo, times(1)).getOne(UNIT_TYPE_ID);
        verify(factionRepository, times(1)).getById(factionId);
        var captor = ArgumentCaptor.forClass(FactionUnitType.class);
        verify(factionUnitTypeRepository, times(1)).save(captor.capture());
        var result = captor.getValue();
        assertEquals(unitType, result.getUnitType());
        assertEquals(givenFaction, result.getFaction());
        assertNull(result.getId());
        assertEquals(OVERRIDE_MAX_COUNT, result.getMaxCount());
    }
}
