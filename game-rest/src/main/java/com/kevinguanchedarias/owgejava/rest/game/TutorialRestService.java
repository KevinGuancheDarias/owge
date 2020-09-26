package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.TutorialSectionBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionEntryDto;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("game/tutorial")
@ApplicationScope
public class TutorialRestService {

	@Autowired
	private TutorialSectionBo tutorialSectionBo;

	@Autowired
	private UserStorageBo userStorageBo;

	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("entries")
	public List<TutorialSectionEntryDto> findEntries() {
		return tutorialSectionBo.findEntries();
	}

	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("visited-entries")
	public List<Long> findVisitedEntries() {
		return tutorialSectionBo.findVisitedIdsByUser(userStorageBo.findLoggedIn().getId());
	}

	/**
	 *
	 * @param entryId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("visited-entries")
	public void addVisitedEntry(@RequestBody Long entryId) {
		tutorialSectionBo.addVisitedEntry(userStorageBo.findLoggedIn().getId(), entryId);
	}

}
