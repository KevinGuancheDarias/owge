package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.TutorialSectionBo;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionAvailableHtmlSymbolDto;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionDto;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionEntryDto;
import com.kevinguanchedarias.owgejava.entity.TutorialSection;
import com.kevinguanchedarias.owgejava.repository.TutorialSectionEntryRepository;
import com.kevinguanchedarias.owgejava.repository.TutorialSectionRepository;
import com.kevinguanchedarias.owgejava.rest.trait.WithReadRestServiceTrait;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/tutorial_section")
@AllArgsConstructor
public class AdminTutorialSectionRestService
        implements WithReadRestServiceTrait<Integer, TutorialSection, TutorialSectionRepository, TutorialSectionDto> {

    private final TutorialSectionRepository tutorialSectionRepository;
    private final AutowireCapableBeanFactory beanFactory;
    private final TutorialSectionBo tutorialSectionBo;
    private final TutorialSectionEntryRepository entryRepository;

    @Override
    @Transactional
    public RestCrudConfigBuilder<Integer, TutorialSection, TutorialSectionRepository, TutorialSectionDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, TutorialSection, TutorialSectionRepository, TutorialSectionDto> builder = RestCrudConfigBuilder
                .create();
        return builder.withBeanFactory(beanFactory).withRepository(tutorialSectionRepository)
                .withDtoClass(TutorialSectionDto.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withReadAll().withReadById());
    }

    /**
     * Finds all html symbols
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostMapping("entries")
    @PutMapping("entries")
    public TutorialSectionEntryDto addUpdateEntry(@RequestBody TutorialSectionEntryDto pojo) {
        return tutorialSectionBo.addUpdateEntry(pojo);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @DeleteMapping("entries/{entryId}")
    public void deleteEntry(@PathVariable Long entryId) {
        entryRepository.deleteById(entryId);
    }
}
