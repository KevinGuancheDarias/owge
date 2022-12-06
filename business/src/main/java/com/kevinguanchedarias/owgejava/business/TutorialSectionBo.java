package com.kevinguanchedarias.owgejava.business;

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
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all the things related with the TutorialSections
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
@AllArgsConstructor
public class TutorialSectionBo implements BaseBo<Integer, TutorialSection, TutorialSectionDto> {
    public static final String TUTORIAL_SECTION_CACHE_TAG = "tutorial_section";

    @Serial
    private static final long serialVersionUID = -1495931971344790940L;

    private final transient TutorialSectionRepository repository;
    private final transient TutorialSectionEntryRepository entryRepository;
    private final transient TutorialSectionAvailableHtmlSymbolRepository htmlSymbolRepository;
    private final transient VisitedTutorialSectionEntryRepository visitedTutorialSectionEntryRepository;
    private final UserStorageBo userStorageBo;
    private final TranslationBo translationBo;
    private final transient SocketIoService socketIoService;
    private final transient DtoUtilService dtoUtilService;

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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional
    public List<TutorialSectionDto> findAllHydrated() {
        return repository.findAll().stream().map(this::hydrate).collect(Collectors.toList());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<TutorialSectionAvailableHtmlSymbolDto> findAvailableHtmlSymbols() {
        return htmlSymbolRepository.findAll().stream()
                .map(current -> dtoUtilService.dtoFromEntity(TutorialSectionAvailableHtmlSymbolDto.class, current))
                .collect(Collectors.toList());
    }

    /**
     * Finds all the tutorial entries
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<TutorialSectionEntryDto> findEntries() {
        return entryRepository.findAll(Sort.by("order").ascending()).stream()
                .map(current -> dtoUtilService.dtoFromEntity(TutorialSectionEntryDto.class, current))
                .collect(Collectors.toList());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional
    public TutorialSectionDto findOneHydratedById(Integer id) {
        TutorialSection tutorialSection = findByIdOrDie(id);
        return hydrate(tutorialSection);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<Long> findVisitedIdsByUser(Integer userId) {
        return visitedTutorialSectionEntryRepository.findVisitedByUserId(userId).stream()
                .map(current -> current.getEntry().getId()).collect(Collectors.toList());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
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
    

    private TutorialSectionDto hydrate(TutorialSection tutorialSection) {
        Hibernate.initialize(tutorialSection.getAvailableHtmlSymbols());
        return toDto(tutorialSection);
    }
}
