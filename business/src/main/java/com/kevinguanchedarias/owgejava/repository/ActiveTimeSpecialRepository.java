/**
 * 
 */
package com.kevinguanchedarias.owgejava.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface ActiveTimeSpecialRepository extends JpaRepository<ActiveTimeSpecial, Long> {

	/**
	 * 
	 * @param userId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ActiveTimeSpecial> findByUserId(Integer userId);

	/**
	 * 
	 * @param userId
	 * @param state
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ActiveTimeSpecial> findByUserIdAndState(Integer userId, TimeSpecialStateEnum state);

	/**
	 * 
	 * @param timeSpecialId
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void deleteByTimeSpecialId(Integer timeSpecialId);

	/**
	 * 
	 * @param timeSpecialId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ActiveTimeSpecial findOneByTimeSpecialId(Integer timeSpecialId);
}
