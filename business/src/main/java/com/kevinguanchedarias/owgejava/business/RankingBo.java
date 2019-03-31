/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.RankingEntry;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class RankingBo {

	@Autowired
	private UserStorageRepository userStorageRepository;

	/**
	 * Find all the ranking entries <br>
	 * <b>NOTICE:</b> NOT using pagination
	 * 
	 * @return
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<RankingEntry> findRanking() {
		final AtomicInteger position = new AtomicInteger(1);
		return userStorageRepository.findAllByOrderByPointsDesc().stream().map(current -> {
			RankingEntry retVal = new RankingEntry(position.get(), current.getPoints(), current.getId(),
					current.getUsername());
			defineAlliance(retVal, current);
			position.incrementAndGet();
			return retVal;
		}).collect(Collectors.toList());
	}

	/**
	 * Defines the alliance
	 * 
	 * @param input
	 *            ranking entry to fill
	 * @param inputUser
	 *            user to use data from
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void defineAlliance(RankingEntry input, UserStorage inputUser) {
		Alliance alliance = inputUser.getAlliance();
		if (alliance != null) {
			input.setAllianceId(alliance.getId());
			input.setAllianceName(alliance.getName());
		}
	}
}
