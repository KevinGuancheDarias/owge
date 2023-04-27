/**
 *
 */
package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public interface ActiveTimeSpecialRepository extends JpaRepository<ActiveTimeSpecial, Long> {

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    List<ActiveTimeSpecial> findByUserId(Integer userId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    List<ActiveTimeSpecial> findByUserIdAndState(Integer userId, TimeSpecialStateEnum state);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    void deleteByTimeSpecialId(Integer timeSpecialId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    Optional<ActiveTimeSpecial> findOneByTimeSpecialIdAndUserId(Integer timeSpecialId, Integer userId);
    
    void deleteByUser(UserStorage user);
}
