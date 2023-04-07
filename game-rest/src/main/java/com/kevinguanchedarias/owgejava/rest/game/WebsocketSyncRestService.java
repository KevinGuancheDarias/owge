package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.business.WebsocketSyncService;
import com.kevinguanchedarias.owgejava.business.audit.AuditBo;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import lombok.AllArgsConstructor;
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
        auditBo.doAudit(AuditActionEnum.LOGIN);
        return websocketSyncService.findWantedData(keys);
    }

}
