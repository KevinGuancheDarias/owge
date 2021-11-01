package com.kevinguanchedarias.owgejava.converter.faction;

import com.kevinguanchedarias.owgejava.dto.FactionSpawnLocationDto;
import com.kevinguanchedarias.owgejava.entity.FactionSpawnLocation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FactionSpawnLocationEntityToDtoConverter implements Converter<FactionSpawnLocation, FactionSpawnLocationDto> {
    @Override
    public FactionSpawnLocationDto convert(FactionSpawnLocation source) {
        return FactionSpawnLocationDto.builder()
                .galaxyId(source.getGalaxy().getId())
                .sectorRangeStart(source.getSectorRangeStart())
                .sectorRangeEnd(source.getSectorRangeEnd())
                .quadrantRangeStart(source.getQuadrantRangeStart())
                .quadrantRangeEnd(source.getQuadrantRangeEnd())
                .build();
    }
}
