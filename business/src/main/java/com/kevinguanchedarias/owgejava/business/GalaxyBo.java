package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.GalaxyDto;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.ArrayList;

@Service
@AllArgsConstructor
public class GalaxyBo implements WithNameBo<Integer, Galaxy, GalaxyDto> {
    @Serial
    private static final long serialVersionUID = 5691936505840441041L;

    private static final Long GALAXY_MAX_LENGTH = 50000L;
    private static final int[] RICHNESS_POSSIBILITIES = new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

    private final GalaxyRepository galaxyRepository;
    private final PlanetRepository planetRepository;
    
    @Override
    public JpaRepository<Galaxy, Integer> getRepository() {
        return galaxyRepository;
    }

    @Override
    public Class<GalaxyDto> getDtoClass() {
        return GalaxyDto.class;
    }

    @Transactional
    public Galaxy save(Galaxy galaxy) {
        canSave(galaxy);

        prepareGalaxy(galaxy);

        return galaxyRepository.saveAndFlush(galaxy);
    }


    /**
     * Returns the coordinates as a string with their numeric value
     * <br>
     * for example for galaxyId 2, sector 4, quadrant 1, would return <i>2-4-1</i>
     */
    public String coordinatesToString(int galaxyId, long sector, long quadrant) {
        return String.valueOf(galaxyId) + '-' + sector + '-' + quadrant;
    }

    /**
     * Returns number of planets galaxy will have
     *
     * @author Kevin Guanche Darias
     */
    public Long computedPlanetsCount(Galaxy galaxy) {
        return galaxy.getSectors() * galaxy.getQuadrants() * galaxy.getNumPlanets();
    }

    /**
     * Will check if it's possible to save the galaxy
     *
     * @throws SgtBackendInvalidInputException When it's not possible to save
     * @author Kevin Guanche Darias
     */
    private void canSave(Galaxy galaxy) {
        checkInput(galaxy);
        checkUnused(galaxy);
    }

    private void checkInput(Galaxy galaxy) {
        if (galaxy.getSectors() < 1 || galaxy.getQuadrants() < 1 || galaxy.getNumPlanets() < 1) {
            throw new SgtBackendInvalidInputException("Invalid input");
        }

        if (computedPlanetsCount(galaxy) > GALAXY_MAX_LENGTH) {
            throw new SgtBackendInvalidInputException(
                    "Galaxy can't have more than " + GALAXY_MAX_LENGTH + " planets");
        }
    }

    /**
     * Will check if the selected galaxy is empty Considered empty when there are
     * no players in it
     *
     * @author Kevin Guanche Darias
     */
    private void checkUnused(Galaxy galaxy) {
        if (planetRepository.findOneByGalaxyIdAndOwnerNotNullOrderByGalaxyId(galaxy.getId()) != null) {
            throw new SgtBackendInvalidInputException("Can't modify a galaxy that already has players");
        }
    }

    /**
     * Will append a transient planet instance to the galaxy
     *
     * @author Kevin Guanche Darias
     */
    private void preparePlanet(Galaxy galaxy, int sector, int quadrant,
                               int planetNumber) {
        var planet = new Planet();
        planet.setName(galaxy.getName().charAt(0) + "S" + sector + "C" + quadrant + "N" + planetNumber);
        planet.setRichness(RICHNESS_POSSIBILITIES[RandomUtils.nextInt(0, RICHNESS_POSSIBILITIES.length)]);
        planet.setGalaxy(galaxy);
        planet.setSector((long) sector);
        planet.setQuadrant((long) quadrant);
        planet.setPlanetNumber(planetNumber);

        if (galaxy.getPlanets() == null) {
            galaxy.setPlanets(new ArrayList<>());
        }

        galaxy.getPlanets().add(planet);
    }

    /**
     * Prepares the galaxy for saving, so if galaxy has never been persisted will
     * insert ALL its planets WARNING: HEAVY INTENSE OPERATION!
     *
     * @author Kevin Guanche Darias
     */
    private void prepareGalaxy(Galaxy galaxy) {
        if (galaxy.getId() != null) {
            planetRepository.deleteByGalaxyId(galaxy.getId());
        }

        for (int sector = 1; sector <= galaxy.getSectors(); sector++) {
            for (int quadrant = 1; quadrant <= galaxy.getQuadrants(); quadrant++) {
                for (int planetNumber = 1; planetNumber <= galaxy.getNumPlanets(); planetNumber++) {
                    preparePlanet(galaxy, sector, quadrant, planetNumber);
                }
            }
        }
    }
}
