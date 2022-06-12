package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.converter.faction.FactionSpawnLocationDtoToEntityConverter;
import com.kevinguanchedarias.owgejava.converter.faction.FactionSpawnLocationEntityToDtoConverter;
import com.kevinguanchedarias.owgejava.entity.FactionSpawnLocation;
import com.kevinguanchedarias.owgejava.mock.GalaxyMock;
import com.kevinguanchedarias.owgejava.repository.FactionRepository;
import com.kevinguanchedarias.owgejava.repository.FactionSpawnLocationRepository;
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

import static com.kevinguanchedarias.owgejava.mock.FactionMock.QUADRANT_RANGE_END;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.QUADRANT_RANGE_START;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.SECTOR_RANGE_END;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.SECTOR_RANGE_START;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenFaction;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenSpawnLocation;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenSpawnLocationDto;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                FactionSpawnLocationBo.class,
                FactionSpawnLocationEntityToDtoConverter.class,
                FactionSpawnLocationDtoToEntityConverter.class,
                DefaultConversionService.class
        }
)
@MockBean({
        FactionSpawnLocationRepository.class,
        GalaxyBo.class,
        FactionRepository.class
})
class FactionSpawnLocationBoTest {

    private final FactionSpawnLocationRepository factionSpawnLocationRepository;
    private final FactionSpawnLocationBo factionSpawnLocationBo;
    private final FactionRepository factionRepository;
    private final GalaxyBo galaxyBo;

    @Autowired
    FactionSpawnLocationBoTest(
            FactionSpawnLocationRepository factionSpawnLocationRepository,
            FactionSpawnLocationBo factionSpawnLocationBo,
            FactionRepository factionRepository,
            GalaxyBo galaxyBo,
            DefaultConversionService conversionService,
            Collection<Converter<?, ?>> converters
    ) {
        this.factionSpawnLocationRepository = factionSpawnLocationRepository;
        this.factionSpawnLocationBo = factionSpawnLocationBo;
        this.factionRepository = factionRepository;
        this.galaxyBo = galaxyBo;
        converters.forEach(conversionService::addConverter);
    }

    @Test
    void findByFaction_should_work() {
        int factionId = 1;
        var entity = givenSpawnLocation();
        when(this.factionSpawnLocationRepository.findByFactionId(factionId)).thenReturn(List.of(entity));

        var result = this.factionSpawnLocationBo.findByFaction(factionId);

        assertThat(result).isNotEmpty();
        var dto = result.get(0);
        assertEquals(GALAXY_ID, dto.getGalaxyId());
        assertEquals(SECTOR_RANGE_START, dto.getSectorRangeStart());
        assertEquals(SECTOR_RANGE_END, dto.getSectorRangeEnd());
        assertEquals(QUADRANT_RANGE_START, dto.getQuadrantRangeStart());
        assertEquals(QUADRANT_RANGE_END, dto.getQuadrantRangeEnd());
    }

    @Test
    void saveSpawnLocations_should_delete_old_and_save_new() {
        var factionId = 1;
        var faction = givenFaction();
        var galaxy = GalaxyMock.givenGalaxy();
        var spawnLocation = givenSpawnLocationDto();
        given(galaxyBo.getOne(GALAXY_ID)).willReturn(galaxy);
        given(factionRepository.getById(factionId)).willReturn(faction);

        factionSpawnLocationBo.saveSpawnLocations(factionId, List.of(spawnLocation));

        verify(galaxyBo, times(1)).getOne(GALAXY_ID);
        var captor = ArgumentCaptor.forClass(FactionSpawnLocation.class);
        verify(factionSpawnLocationRepository, times(1)).save(captor.capture());
        var saved = captor.getValue();
        assertEquals(galaxy, saved.getGalaxy());
        assertEquals(faction, saved.getFaction());
        assertEquals(SECTOR_RANGE_START, saved.getSectorRangeStart());
        assertEquals(SECTOR_RANGE_END, saved.getSectorRangeEnd());
        assertEquals(QUADRANT_RANGE_START, saved.getQuadrantRangeStart());
        assertEquals(QUADRANT_RANGE_END, saved.getQuadrantRangeEnd());
    }

    @Test
    void determineSpawnGalaxy_should_return_null_when_faction_has_not_defined_custom_spawns() {
        assertNull(factionSpawnLocationBo.determineSpawnGalaxy(givenFaction()));
    }

    @Test
    void determineSpawnGalaxy_should_return_random_custom_galaxy() {
        var faction = givenFaction();
        var ids = List.of(1, 2, 3);
        when(factionSpawnLocationRepository.findSpawnGalaxiesByFaction(faction)).thenReturn(ids);

        assertThat(this.factionSpawnLocationBo.determineSpawnGalaxy(faction)).isIn(ids);
        verify(factionSpawnLocationRepository, times(1)).findSpawnGalaxiesByFaction(faction);

    }

}
