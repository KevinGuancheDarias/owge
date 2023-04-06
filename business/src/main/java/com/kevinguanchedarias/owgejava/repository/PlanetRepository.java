package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface PlanetRepository extends WithNameRepository<Planet, Long>, Serializable {


    Planet findOneByGalaxyIdAndOwnerNotNullOrderByGalaxyId(Integer galaxyId);

    long countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(Integer galaxyId);

    long countByOwnerIsNullAndSpecialLocationIsNull();

    List<Planet> findByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(Integer galaxyId, Pageable pageable);

    List<Planet> findByOwnerIsNullAndSpecialLocationIsNull(Pageable pageable);

    Optional<Planet> findOneByIdAndOwnerId(Long planetId, Integer ownerId);

    Planet findOneByOwnerIdAndHomeTrue(Integer ownerId);

    @Query("SELECT case when count(p)> 0 then true else false end FROM Planet p WHERE p.id = ?2 AND p.owner.id = ?1 ")
    boolean isOfUserProperty(Integer ownerId, Long planetId);

    @Query("SELECT case when count(p)> 0 then true else false end FROM Planet p WHERE p = ?2 AND p.owner = ?1 ")
    boolean isOfUserProperty(UserStorage user, Planet planet);

    List<Planet> findByOwnerId(Integer ownerId);

    int countByOwnerId(Integer ownerId);

    List<Planet> findByGalaxyIdAndSectorAndQuadrant(Integer galaxy, Long sector, Long quadrant);

    Planet findOneByIdAndHomeTrue(Long planetId);

    /**
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    List<Planet> findByGalaxyIdAndOwnerNotNull(Integer galaxyId);

    /**
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    Planet findOneBySpecialLocationId(Integer specialLocationId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.14
     */
    void deleteByGalaxyId(Integer galaxyId);

    @Query("UPDATE Planet SET specialLocation = ?2 WHERE id = ?1")
    @Modifying
    void updateSpecialLocation(long planetId, SpecialLocation specialLocation);
}