package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.WebsocketEventsInformationDto;
import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.repository.WebsocketEventsInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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

	@Override
	public Class<WebsocketEventsInformationDto> getDtoClass() {
		return WebsocketEventsInformationDto.class;
	}

	/**
	 *
	 * @param userId
	 * @return
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
		repository.truncateTable();
	}

	/**
	 *
	 * @param websocketEventsInformation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.SUPPORTS)
	public void save(WebsocketEventsInformation websocketEventsInformation) {
		Optional<WebsocketEventsInformation> existing = repository
				.findById(websocketEventsInformation.getEventNameUserId());
		if (existing.isPresent()) {
			WebsocketEventsInformation existingEntity = existing.get();
			existingEntity.setLastSenT(new Date());
			repository.save(existingEntity);
		} else {
			repository.save(websocketEventsInformation);
		}
	}
}
