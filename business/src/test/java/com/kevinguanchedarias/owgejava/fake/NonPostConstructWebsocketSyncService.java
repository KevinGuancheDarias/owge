package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.WebsocketEventsInformationBo;
import com.kevinguanchedarias.owgejava.business.WebsocketSyncService;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@Primary
public class NonPostConstructWebsocketSyncService extends WebsocketSyncService {
    public NonPostConstructWebsocketSyncService(List<SyncSource> syncSources, UserSessionService userSessionService, WebsocketEventsInformationBo websocketEventsInformationBo) {
        super(syncSources, userSessionService, websocketEventsInformationBo);
    }

    @Override
    @PostConstruct
    public void init() {
        // Does nothing
    }

    public void invokeRealInit() {
        super.init();
    }
}
