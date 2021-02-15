package com.kevinguanchedarias.owgejava.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.SystemMessage;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface SystemMessageRepository extends JpaRepository<SystemMessage, Integer> {

	/**
	 *
	 * @param date
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	List<SystemMessage> findByCreationDateLessThan(Date date);

}
