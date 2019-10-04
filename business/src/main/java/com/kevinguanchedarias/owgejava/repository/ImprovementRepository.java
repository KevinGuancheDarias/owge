/**
 * 
 */
package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.Improvement;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface ImprovementRepository extends JpaRepository<Improvement, Number>, Serializable {

}
