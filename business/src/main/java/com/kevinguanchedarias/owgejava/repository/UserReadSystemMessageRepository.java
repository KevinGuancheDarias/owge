package com.kevinguanchedarias.owgejava.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.UserReadSystemMessage;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface UserReadSystemMessageRepository extends JpaRepository<UserReadSystemMessage, Long> {
	/**
	 *
	 * @param messageId
	 * @param userId
	 * @return
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean existsByMessageIdAndUserId(Integer messageId, Integer userId);
}
