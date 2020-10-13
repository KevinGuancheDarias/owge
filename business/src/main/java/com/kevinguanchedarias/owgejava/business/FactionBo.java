package com.kevinguanchedarias.owgejava.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.FactionRepository;

@Service
public class FactionBo implements BaseBo<Integer, Faction, FactionDto> {
	private static final long serialVersionUID = -6735454832872729630L;

	@Autowired
	private FactionRepository factionRepository;

	@Override
	public JpaRepository<Faction, Integer> getRepository() {
		return factionRepository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<FactionDto> getDtoClass() {
		return FactionDto.class;
	}

	/**
	 * Returns the factions that are visible
	 *
	 * @param lazyFetch Fetch the proxies, or set to null
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<Faction> findVisible(boolean lazyFetch) {
		List<Faction> retVal = factionRepository.findByHiddenFalse();
		handleLazyFetch(lazyFetch, retVal);
		return retVal;
	}

	/**
	 * Will check if given faction exists AND it's visible
	 *
	 * @param id faction id
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public boolean existsAndIsVisible(Integer id) {
		return factionRepository.countByHiddenFalseAndId(id) == 1;
	}

	@Override
	public Faction save(Faction faction) {
		Float customPrimaryGatherPercentage = faction.getCustomPrimaryGatherPercentage();
		customPrimaryGatherPercentage = customPrimaryGatherPercentage != null ? customPrimaryGatherPercentage : 1;
		Float customSecondaryGatherPercentage = faction.getCustomSecondaryGatherPercentage();
		customSecondaryGatherPercentage = customSecondaryGatherPercentage != null ? customSecondaryGatherPercentage : 1;
		if ((customPrimaryGatherPercentage + customSecondaryGatherPercentage) > 0) {
			if ((customPrimaryGatherPercentage + customSecondaryGatherPercentage) > 100) {
				throw new SgtBackendInvalidInputException(
						"No, dear hacker, custom primary porcentage plus secondary CAN'T be higher than 100");
			}
		}
		return BaseBo.super.save(faction);
	}

	private void handleLazyFetch(boolean lazyFetch, List<Faction> factions) {
		if (lazyFetch) {
			for (Faction current : factions) {
				current.getImprovement().getId();
			}
		} else {
			for (Faction current : factions) {
				current.setImprovement(null);
			}
		}
	}

}
