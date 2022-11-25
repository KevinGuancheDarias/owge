package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.GalaxyDto;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNoGalaxiesFound;
import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.ArrayList;

@Service
public class GalaxyBo implements WithNameBo<Integer, Galaxy, GalaxyDto> {
    public static final String GALAXY_CACHE_TAG = "galaxy";

    @Serial
    private static final long serialVersionUID = 5691936505840441041L;

    private static final Long GALAXY_MAX_LENGTH = 50000L;

    @Autowired
    private GalaxyRepository galaxyRepository;

    @Autowired
    private PlanetRepository planetRepository;

    @Autowired
    private PlanetBo planetBo;

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
     * Will check if it's possible to save the galaxy
     *
     * @throws SgtBackendInvalidInputException When it's not possible to save
     * @author Kevin Guanche Darias
     */
    public void canSave(Galaxy galaxy) {
        checkInput(galaxy);
        checkUnused(galaxy);
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
     * @return Returns a random galaxy
     * @author Kevin Guanche Darias
     */
    public Integer findRandomGalaxy() {
        int count = (int) countAll();

        if (count == 0) {
            throw new SgtBackendNoGalaxiesFound("Este universo no posee galaxias");
        }

        var selectedGalaxy = RandomUtils.nextInt(0, count);

        return galaxyRepository.findAll(PageRequest.of(selectedGalaxy, 1)).getContent().get(0).getId();
    }

    /**
     * Returns true if the specified galaxy has players
     *
     * @param id Galaxy id
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public boolean hasPlayers(Integer id) {
        return !planetRepository.findByGalaxyIdAndOwnerNotNull(id).isEmpty();
    }

    private void checkInput(Galaxy galaxy) {
        if (galaxy.getSectors() < 1 || galaxy.getQuadrants() < 1 || galaxy.getNumPlanets() < 1) {
            throw new SgtBackendInvalidInputException("Datos de entrada no válidos");
        }

        if (computedPlanetsCount(galaxy) > GALAXY_MAX_LENGTH) {
            throw new SgtBackendInvalidInputException(
                    "La galaxia no puede tener más de " + GALAXY_MAX_LENGTH + " planetas");
        }
    }

    /**
     * Will check if the selected galaxy is empty Considered empty when there are
     * not players in it
     *
     * @author Kevin Guanche Darias
     */
    private void checkUnused(Galaxy galaxy) {
        if (planetRepository.findOneByGalaxyIdAndOwnerNotNullOrderByGalaxyId(galaxy.getId()) != null) {
            throw new SgtBackendInvalidInputException("No se alterar una galaxia que ya contiene jugadores");
        }
    }

    /**
     * Will append a transient planet instance to the galaxy
     *
     * @author Kevin Guanche Darias
     */
    private void preparePlanet(Integer[] richnessPosibilities, Galaxy galaxy, int sector, int quadrant,
                               int planetNumber) {
        Planet planet = new Planet();
        planet.setName(galaxy.getName().charAt(0) + "S" + sector + "C" + quadrant + "N" + planetNumber);
        planet.setRichness(richnessPosibilities[RandomUtils.nextInt(0, richnessPosibilities.length)]);
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
            planetBo.deleteByGalaxy(galaxy.getId());
        }
        Integer[] richnessPosibilities = generateRichnessPosibilities();

        for (int sector = 1; sector <= galaxy.getSectors(); sector++) {
            for (int quadrant = 1; quadrant <= galaxy.getQuadrants(); quadrant++) {
                for (int planetNumber = 1; planetNumber <= galaxy.getNumPlanets(); planetNumber++) {
                    preparePlanet(richnessPosibilities, galaxy, sector, quadrant, planetNumber);
                }
            }
        }
    }

    /**
     * Will generate the richness possibilities
     */
    private Integer[] generateRichnessPosibilities() {
        return new Integer[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    }
}
