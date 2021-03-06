/**
 * 
 */
package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface AllianceJoinRequestRepository extends JpaRepository<AllianceJoinRequest, Integer>, Serializable {
	AllianceJoinRequest findOneByUserAndAlliance(UserStorage user, Alliance alliance);

	List<AllianceJoinRequest> findByAlliance(Alliance alliance);

	/**
	 * @param id
	 * @return
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	void deleteByUserId(Number id);

	/**
	 * 
	 * @param id
	 * @return
	 * @author Kevin Guanche Darias
	 * @since 0.8.1
	 */
	List<AllianceJoinRequest> findByUserId(Integer id);
}
