package com.kevinguanchedarias.owgejava.repository;

import java.util.List;

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

	/**
	 *
	 * @param userId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<WebsocketEventsInformation> findByEventNameUserIdUserId(Integer userId);

	/**
	 *
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Modifying
	@Query(value = "TRUNCATE TABLE websocket_events_information", nativeQuery = true)
	public void truncateTable();
}
