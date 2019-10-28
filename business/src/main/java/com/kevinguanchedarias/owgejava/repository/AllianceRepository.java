/**
 * 
 */
package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import com.kevinguanchedarias.owgejava.entity.Alliance;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface AllianceRepository extends WithNameRepository<Alliance, Integer>, Serializable {
	Alliance findOneByOwnerId(Number userId);
}
