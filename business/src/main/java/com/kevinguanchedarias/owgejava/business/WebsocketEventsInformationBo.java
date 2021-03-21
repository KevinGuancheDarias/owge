package com.kevinguanchedarias.owgejava.business;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.WebsocketEventsInformationDto;
import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.entity.embeddedid.EventNameUserId;
import com.kevinguanchedarias.owgejava.repository.WebsocketEventsInformationRepository;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
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
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<WebsocketEventsInformation> findByUserId(Integer userId) {
		return repository.findByEventNameUserIdUserId(userId);
	}

	/**
	 * Removes all entries
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void clear() {
		Date date = new Date();
		userStorageBo.findAllIds().forEach(userId -> repository.updateLastSent(userId, date));
	}

	/**
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
