package com.kevinguanchedarias.owgejava.business.planet;


import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlanetCleanerService {
    private final PlanetExplorationService planetExplorationService;

    public void cleanUpUnexplored(Integer userId, PlanetDto planetDto) {
        if (!planetExplorationService.isExplored(userId, planetDto.getId())) {
            planetDto.setName(null);
            planetDto.setRichness(null);
            planetDto.setHome(null);
            planetDto.setOwnerId(null);
            planetDto.setOwnerName(null);
            planetDto.setSpecialLocation(null);
        }
    }
}
