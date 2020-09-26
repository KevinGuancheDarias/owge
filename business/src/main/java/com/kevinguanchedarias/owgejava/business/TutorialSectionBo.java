package com.kevinguanchedarias.owgejava.business;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.TutorialSectionAvailableHtmlSymbolDto;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionDto;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionEntryDto;
import com.kevinguanchedarias.owgejava.entity.TutorialSection;
import com.kevinguanchedarias.owgejava.entity.TutorialSectionEntry;
import com.kevinguanchedarias.owgejava.entity.VisitedTutorialSectionEntry;
import com.kevinguanchedarias.owgejava.repository.TutorialSectionAvailableHtmlSymbolRepository;
import com.kevinguanchedarias.owgejava.repository.TutorialSectionEntryRepository;
import com.kevinguanchedarias.owgejava.repository.TutorialSectionRepository;
import com.kevinguanchedarias.owgejava.repository.VisitedTutorialSectionEntryRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;

/**
 * Handles all the things related with the TutorialSections
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
public class TutorialSectionBo implements BaseBo<Integer, TutorialSection, TutorialSectionDto> {
	private static final long serialVersionUID = -1495931971344790940L;

	@Autowired
	private transient TutorialSectionRepository repository;

	@Autowired
	private transient TutorialSectionEntryRepository entryRepository;

	@Autowired
	private transient TutorialSectionAvailableHtmlSymbolRepository htmlSymbolRepository;

	@Autowired
	private VisitedTutorialSectionEntryRepository visitedTutorialSectionEntryRepository;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private TranslationBo translationBo;

	@Autowired
	private SocketIoService socketIoService;

	@Autowired
	private transient DtoUtilService dtoUtilService;

	@Override
	public Class<TutorialSectionDto> getDtoClass() {
		return TutorialSectionDto.class;
	}

	@Override
	public JpaRepository<TutorialSection, Integer> getRepository() {
		return repository;
	}

	/**
	 * Returns all tutorial sections, with their available symbols
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public List<TutorialSectionDto> findAllHydrated() {
		return repository.findAll().stream().map(this::hydrate).collect(Collectors.toList());
	}

	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<TutorialSectionAvailableHtmlSymbolDto> findAvailableHtmlSymbols() {
		return htmlSymbolRepository.findAll().stream()
				.map(current -> dtoUtilService.dtoFromEntity(TutorialSectionAvailableHtmlSymbolDto.class, current))
				.collect(Collectors.toList());
	}

	/**
	 * Finds all the tutorial entries
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<TutorialSectionEntryDto> findEntries() {
		return entryRepository.findAll(Sort.by("order").ascending()).stream()
				.map(current -> dtoUtilService.dtoFromEntity(TutorialSectionEntryDto.class, current))
				.collect(Collectors.toList());
	}

	/**
	 *
	 * @param id
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public TutorialSectionDto findOneHydratedById(Integer id) {
		TutorialSection tutorialSection = findByIdOrDie(id);
		return hydrate(tutorialSection);
	}

	/**
	 *
	 * @param userId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<Long> findVisitedIdsByUser(Integer userId) {
		return visitedTutorialSectionEntryRepository.findVisitedByUserId(userId).stream()
				.map(current -> current.getEntry().getId()).collect(Collectors.toList());
	}

	/**
	 *
	 * @param userId
	 * @param entryId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void addVisitedEntry(Integer userId, Long entryId) {
		VisitedTutorialSectionEntry visitedEntry = new VisitedTutorialSectionEntry();
		visitedEntry.setUser(userStorageBo.findByIdOrDie(userId));
		visitedEntry.setEntry(entryRepository.findById(entryId).get());
		visitedTutorialSectionEntryRepository.save(visitedEntry);
		TransactionUtil.doAfterCommit(() -> {
			socketIoService.sendMessage(userId, "visited_tutorial_entry_change", () -> findVisitedIdsByUser(userId));
		});
	}

	/**
	 *
	 * @param entryDto
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public TutorialSectionEntryDto addUpdateEntry(TutorialSectionEntryDto entryDto) {
		TutorialSectionEntry entity = entryDto.getId() == null ? new TutorialSectionEntry()
				: entryRepository.findById(entryDto.getId()).orElse(new TutorialSectionEntry());
		entity.setEvent(entryDto.getEvent());
		entity.setHtmlSymbol(htmlSymbolRepository.findById(entryDto.getHtmlSymbol().getId()).orElseThrow());
		entity.setOrder(entryDto.getOrder());
		entity.setText(translationBo.findByIdOrDie(entryDto.getText().getId()));
		return dtoUtilService.dtoFromEntity(TutorialSectionEntryDto.class, entryRepository.save(entity));
	}

	/**
	 *
	 * @param entryId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void deleteEntry(Long entryId) {
		entryRepository.deleteById(entryId);
	}

	private TutorialSectionDto hydrate(TutorialSection tutorialSection) {
		Hibernate.initialize(tutorialSection.getAvailableHtmlSymbols());
		return toDto(tutorialSection);
	}
}
