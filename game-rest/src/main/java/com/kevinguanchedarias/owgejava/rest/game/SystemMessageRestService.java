package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.SystemMessageBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 */
@RestController
@RequestMapping("game/system-message")
@ApplicationScope
@AllArgsConstructor
public class SystemMessageRestService implements SyncSource {
    private final SystemMessageBo bo;
    private final UserSessionService userSessionService;

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("system_message_change", user -> bo.findReadByUser(user.getId()))
                .build();
    }

    @PostMapping("mark-as-read")
    public void markAsRead(@RequestBody List<Integer> messages) {
        bo.markAsRead(messages, userSessionService.findLoggedInWithDetails());
    }

}
