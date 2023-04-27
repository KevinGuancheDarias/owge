package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.entity.embeddedid.EventNameUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
public interface WebsocketEventsInformationRepository
        extends JpaRepository<WebsocketEventsInformation, EventNameUserId> {

    Optional<WebsocketEventsInformation> findOneByEventNameUserIdEventNameAndEventNameUserIdUserId(String event,
                                                                                                   Integer userId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    List<WebsocketEventsInformation> findByEventNameUserIdUserId(Integer userId);

    @Modifying
    @Query("UPDATE WebsocketEventsInformation wei SET wei.lastSent = ?2 WHERE wei.eventNameUserId.userId = ?1")
    void updateLastSent(Integer userId, Instant lastSentDate);

    void deleteByEventNameUserIdUserId(Integer userId);
}
