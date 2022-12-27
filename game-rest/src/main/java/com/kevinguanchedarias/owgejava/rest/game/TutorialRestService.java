package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.TutorialSectionBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.dto.TutorialSectionEntryDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@RestController
@RequestMapping("game/tutorial")
@ApplicationScope
@AllArgsConstructor
public class TutorialRestService implements SyncSource {
    private final TutorialSectionBo tutorialSectionBo;
    private final UserSessionService userSessionService;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @GetMapping("entries")
    public List<TutorialSectionEntryDto> findEntries() {
        return tutorialSectionBo.findEntries();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostMapping("visited-entries")
    public void addVisitedEntry(@RequestBody Long entryId) {
        tutorialSectionBo.addVisitedEntry(userSessionService.findLoggedIn().getId(), entryId);
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("tutorial_entries_change", this::findEntries).withHandler(
                "visited_tutorial_entry_change", user -> tutorialSectionBo.findVisitedIdsByUser(user.getId())).build();
    }

}
