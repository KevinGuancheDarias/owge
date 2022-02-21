/**
 *
 */
package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface ActiveTimeSpecialRepository extends JpaRepository<ActiveTimeSpecial, Long> {

    /**
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    List<ActiveTimeSpecial> findByUserId(Integer userId);

    /**
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    List<ActiveTimeSpecial> findByUserIdAndState(Integer userId, TimeSpecialStateEnum state);

    /**
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    void deleteByTimeSpecialId(Integer timeSpecialId);

    /**
     *
     * @since 0.9.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    Optional<ActiveTimeSpecial> findOneByTimeSpecialIdAndUserId(Integer timeSpecialId, Integer userId);
}
