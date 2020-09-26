package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.TutorialSectionBo;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionAvailableHtmlSymbolDto;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionDto;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionEntryDto;
import com.kevinguanchedarias.owgejava.entity.TutorialSection;
import com.kevinguanchedarias.owgejava.rest.trait.WithReadRestServiceTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@ApplicationScope
@RequestMapping("admin/tutorial_section")
public class AdminTutorialSectionRestService
		implements WithReadRestServiceTrait<Integer, TutorialSection, TutorialSectionBo, TutorialSectionDto> {

	@Autowired
	private TutorialSectionBo tutorialSectionBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@Override
	@Transactional
	public RestCrudConfigBuilder<Integer, TutorialSection, TutorialSectionBo, TutorialSectionDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, TutorialSection, TutorialSectionBo, TutorialSectionDto> builder = RestCrudConfigBuilder
				.create();
		return builder.withBeanFactory(beanFactory).withBoService(tutorialSectionBo)
				.withDtoClass(TutorialSectionDto.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withReadAll().withReadById());
	}

	/**
	 * Finds all html symbols
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("availableHtmlSymbols")
	public List<TutorialSectionAvailableHtmlSymbolDto> findHtmlSymbols() {
		return tutorialSectionBo.findAvailableHtmlSymbols();
	}

	@GetMapping("entries")
	public List<TutorialSectionEntryDto> findEntries() {
		return tutorialSectionBo.findEntries();
	}

	/**
	 *
	 * @param sectionId
	 * @param pojo
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("entries")
	@PutMapping("entries")
	public TutorialSectionEntryDto addUpdateEntry(@RequestBody TutorialSectionEntryDto pojo) {
		return tutorialSectionBo.addUpdateEntry(pojo);
	}

	/**
	 *
	 * @param entryId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@DeleteMapping("entries/{entryId}")
	public void deleteEntry(@PathVariable Long entryId) {
		tutorialSectionBo.deleteEntry(entryId);
	}
}
