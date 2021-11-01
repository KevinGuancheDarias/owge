package com.kevinguanchedarias.owgejava.converter.faction;

import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.dto.FactionSpawnLocationDto;
import com.kevinguanchedarias.owgejava.entity.FactionSpawnLocation;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FactionSpawnLocationDtoToEntityConverter implements Converter<FactionSpawnLocationDto, FactionSpawnLocation> {

    private final GalaxyBo galaxyBo;

    @Override
    public FactionSpawnLocation convert(FactionSpawnLocationDto source) {
        return FactionSpawnLocation.builder()
                .galaxy(galaxyBo.getOne(source.getGalaxyId()))
                .sectorRangeStart(source.getSectorRangeStart())
                .sectorRangeEnd(source.getSectorRangeEnd())
                .quadrantRangeStart(source.getQuadrantRangeStart())
                .quadrantRangeEnd(source.getQuadrantRangeEnd())
                .build();
    }
}
