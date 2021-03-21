package com.kevinguanchedarias.owgejava.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.entity.embeddedid.EventNameUserId;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface WebsocketEventsInformationRepository
		extends JpaRepository<WebsocketEventsInformation, EventNameUserId> {

	Optional<WebsocketEventsInformation> findOneByEventNameUserIdEventNameAndEventNameUserIdUserId(String event,
			Integer userId);

	/**
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	List<WebsocketEventsInformation> findByEventNameUserIdUserId(Integer userId);

	@Modifying
	@Query("UPDATE WebsocketEventsInformation wei SET wei.lastSenT = ?2 WHERE wei.eventNameUserId.userId = ?1")
	void updateLastSent(Integer userId, Date lastSentDate);
}
