package com.kevinguanchedarias.owgejava.converter.faction;

import com.kevinguanchedarias.owgejava.dto.FactionSpawnLocationDto;
import com.kevinguanchedarias.owgejava.entity.FactionSpawnLocation;
import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FactionSpawnLocationDtoToEntityConverter implements Converter<FactionSpawnLocationDto, FactionSpawnLocation> {

    private final GalaxyRepository galaxyRepository;

    @Override
    public FactionSpawnLocation convert(FactionSpawnLocationDto source) {
        return FactionSpawnLocation.builder()
                .galaxy(galaxyRepository.getById(source.getGalaxyId()))
                .sectorRangeStart(source.getSectorRangeStart())
                .sectorRangeEnd(source.getSectorRangeEnd())
                .quadrantRangeStart(source.getQuadrantRangeStart())
                .quadrantRangeEnd(source.getQuadrantRangeEnd())
                .build();
    }
}
