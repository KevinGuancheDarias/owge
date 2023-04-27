/**
 *
 */
package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
public interface AllianceJoinRequestRepository extends JpaRepository<AllianceJoinRequest, Integer>, Serializable {
    boolean existsByUserAndAlliance(UserStorage user, Alliance alliance);

    List<AllianceJoinRequest> findByAlliance(Alliance alliance);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    void deleteByUser(UserStorage user);

    /**
     * @author Kevin Guanche Darias
     * @since 0.8.1
     */
    List<AllianceJoinRequest> findByUserId(Integer id);

    void deleteByAlliance(Alliance alliance);
}
