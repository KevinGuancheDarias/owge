package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.WebsocketEventsInformationDto;
import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.entity.embeddedid.EventNameUserId;
import com.kevinguanchedarias.owgejava.repository.WebsocketEventsInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
public class WebsocketEventsInformationBo
        implements WithToDtoTrait<WebsocketEventsInformation, WebsocketEventsInformationDto> {
    @Autowired
    private WebsocketEventsInformationRepository repository;

    @Autowired
    private UserStorageBo userStorageBo;

    @Override
    public Class<WebsocketEventsInformationDto> getDtoClass() {
        return WebsocketEventsInformationDto.class;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<WebsocketEventsInformation> findByUserId(Integer userId) {
        return repository.findByEventNameUserIdUserId(userId);
    }

    /**
     * Removes all entries
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional
    public void clear() {
        userStorageBo.findAllIds().forEach(userId -> repository.updateLastSent(userId, Instant.now()));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public WebsocketEventsInformation save(WebsocketEventsInformation websocketEventsInformation) {
        Optional<WebsocketEventsInformation> existing = repository
                .findById(websocketEventsInformation.getEventNameUserId());
        if (existing.isPresent()) {
            WebsocketEventsInformation existingEntity = existing.get();
            existingEntity.setLastSent(Instant.now().truncatedTo(ChronoUnit.SECONDS));
            return repository.save(existingEntity);
        } else {
            return repository.save(websocketEventsInformation);
        }
    }

    @Transactional
    public void save(String event, Integer userId, Instant lastSent) {
        Optional<WebsocketEventsInformation> existing = repository
                .findOneByEventNameUserIdEventNameAndEventNameUserIdUserId(event, userId);
        if (existing.isPresent()) {
            existing.get().setLastSent(lastSent);
        } else {
            WebsocketEventsInformation eventsInformation = new WebsocketEventsInformation();
            EventNameUserId id = new EventNameUserId();
            id.setEventName(event);
            id.setUserId(userId);
            eventsInformation.setEventNameUserId(id);
            eventsInformation.setLastSent(lastSent);
            save(eventsInformation);
        }
    }
}
