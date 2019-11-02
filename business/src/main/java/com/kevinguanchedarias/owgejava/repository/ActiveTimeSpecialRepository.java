/**
 * 
 */
package com.kevinguanchedarias.owgejava.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface ActiveTimeSpecialRepository extends JpaRepository<ActiveTimeSpecial, Long> {
	public List<ActiveTimeSpecial> findByUserId(Integer userId);

	public ActiveTimeSpecial findOneByTimeSpecialId(Integer timeSpecialId);
}
