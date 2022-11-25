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
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface AllianceJoinRequestRepository extends JpaRepository<AllianceJoinRequest, Integer>, Serializable {
    AllianceJoinRequest findOneByUserAndAlliance(UserStorage user, Alliance alliance);

    List<AllianceJoinRequest> findByAlliance(Alliance alliance);

    /**
     * @since 0.7.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    void deleteByUser(UserStorage user);

    /**
     *
     * @param id
     * @return
     * @author Kevin Guanche Darias
     * @since 0.8.1
     */
    List<AllianceJoinRequest> findByUserId(Integer id);
}
