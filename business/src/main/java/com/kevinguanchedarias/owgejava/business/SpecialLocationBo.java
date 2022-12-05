package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.SpecialLocationDto;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.SpecialLocationRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;

@Service
@AllArgsConstructor
public class SpecialLocationBo implements WithNameBo<Integer, SpecialLocation, SpecialLocationDto> {
    public static final String SPECIAL_LOCATION_CACHE_TAG = "special_location";

    @Serial
    private static final long serialVersionUID = 2511602524693638404L;

    private final SpecialLocationRepository specialLocationRepository;
    private final PlanetRepository planetRepository;
    private final PlanetBo planetBo;

    @Override
    public JpaRepository<SpecialLocation, Integer> getRepository() {
        return specialLocationRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<SpecialLocationDto> getDtoClass() {
        return SpecialLocationDto.class;
    }

    @Transactional
    public SpecialLocation save(SpecialLocation entity) {
        Planet assignedPlanet = null;
        if (entity.getId() != null) {
            var stored = findByIdOrDie(entity.getId());
            if (stored.getGalaxy() == null || !stored.getGalaxy().equals(entity.getGalaxy())) {
                assignedPlanet = assignPlanet(entity);
                if (stored.getAssignedPlanet() != null) {
                    stored.getAssignedPlanet().setSpecialLocation(null);
                    planetRepository.save(stored.getAssignedPlanet());
                }
            }
        }
        if (assignedPlanet != null) {
            entity.setGalaxy(assignedPlanet.getGalaxy());
        } else {
            entity.setGalaxy(null);
        }
        var saved = specialLocationRepository.save(entity);
        if (assignedPlanet != null) {
            assignedPlanet.setSpecialLocation(saved);
            planetRepository.save(assignedPlanet);
        }
        saved.setAssignedPlanet(assignedPlanet);
        return saved;
    }

    @Transactional
    public void delete(SpecialLocation entity) {
        var assignedPlanet = entity.getAssignedPlanet();
        if (assignedPlanet != null) {
            planetRepository.updateSpecialLocation(assignedPlanet.getId(), null);
        }
        specialLocationRepository.delete(entity);
    }

    /**
     * Will return a valid planet for the special location
     *
     * @param specialLocation If the galaxy is null will not be assigned, if 0 will
     *                        choose a random galaxy
     */
    private Planet assignPlanet(SpecialLocation specialLocation) {
        var galaxyId = specialLocation.getGalaxy() != null ? specialLocation.getGalaxy().getId() : null;
        if (galaxyId != null) {
            return planetBo.findRandomPlanet(galaxyId.equals(0) ? null : galaxyId);
        } else {
            return null;
        }
    }

}
