/**
 *
 */
package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
public interface AllianceRepository extends WithNameRepository<Alliance, Integer>, Serializable {
    Alliance findOneByOwnerId(Number userId);

    @Query("SELECT a.users FROM Alliance a WHERE a.id = ?1")
    List<UserStorage> findMembers(Integer id);
}
