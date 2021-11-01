package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.FactionSpawnLocationDto;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.FactionSpawnLocation;
import com.kevinguanchedarias.owgejava.repository.FactionRepository;
import com.kevinguanchedarias.owgejava.repository.FactionSpawnLocationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class FactionSpawnLocationBo {
    private final FactionSpawnLocationRepository repository;
    private final FactionRepository factionRepository;
    private final ConversionService conversionService;

    public List<FactionSpawnLocationDto> findByFaction(int factionId) {
        return repository.findByFactionId(factionId).stream()
                .map(entity -> conversionService.convert(entity, FactionSpawnLocationDto.class))
                .toList();
    }

    @Transactional
    public void saveSpawnLocations(Integer factionId, List<FactionSpawnLocationDto> factionSpawnLocationDtos) {
        repository.deleteByFactionId(factionId);
        factionSpawnLocationDtos.forEach(spawnLocationDto -> {
            var spawnLocation = conversionService.convert(spawnLocationDto, FactionSpawnLocation.class);
            if (spawnLocation != null) {
                spawnLocation.setFaction(factionRepository.getById(factionId));
                repository.save(spawnLocation);
            } else {
                log.warn("Null entity object from DTO: {}", spawnLocationDto);
            }
        });
    }

    public Integer determineSpawnGalaxy(Faction faction) {
        var spawnGalaxies = repository.findSpawnGalaxiesByFaction(faction);
        if (spawnGalaxies.isEmpty()) {
            return null;
        } else {
            return spawnGalaxies.get(RandomUtils.nextInt(0, spawnGalaxies.size()));
        }
    }
}
