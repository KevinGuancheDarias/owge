package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.business.WebsocketSyncService;
import com.kevinguanchedarias.owgejava.business.audit.AuditBo;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Map;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.6
 */
@RestController
@RequestMapping("game/websocket-sync")
@ApplicationScope
@AllArgsConstructor
@Slf4j
public class WebsocketSyncRestService {
    private final WebsocketSyncService websocketSyncService;
    private final AuditBo auditBo;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     */
    @GetMapping
    @Transactional
    public Map<String, Object> sync(@RequestParam List<String> keys) {
        // Best-effort: the audit runs in its own transaction, so if it fails (e.g. the user is
        // authenticated but not yet subscribed to this universe, so the user_storage FK doesn't
        // exist yet) we just skip it instead of breaking the whole sync.
        try {
            auditBo.doBestEffortAudit(AuditActionEnum.LOGIN);
        } catch (RuntimeException e) {
            log.warn("Skipping LOGIN audit, likely the user is not yet registered in this universe: {}", e.getMessage());
        }
        return websocketSyncService.findWantedData(keys);
    }

}
