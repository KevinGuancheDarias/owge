package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.projection.AuditDataProjection;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@AllArgsConstructor
@Log4j2
public class MultiAccountDetectorBo {
    private final UserStorageBo userStorageBo;
    private final AuditBo auditBo;
    private final ConfigurationBo configurationBo;
    private final SocketIoService socketIoService;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = " 0 0 */2 * * ?")
    public void doControlMultiAccounts() {
        log.debug("Scanning multi accounts");
        var checkOffset = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
        userStorageBo.findByLastMultiAccountCheckNewerThan(checkOffset).forEach(user -> {
            Map<String, AtomicInteger> ips = new HashMap<>();
            Map<String, AtomicInteger> userAgents = new HashMap<>();
            Map<String, AtomicInteger> cookies = new HashMap<>();
            handleMyData(auditBo.findDistinctData(user.getId()), ips, userAgents, cookies);
            var relatedAudits = auditBo.findRelated(user);
            var score = new AtomicInteger();
            var torScore = configurationBo.findIntOrSetDefault("MTAS_TOR_SCORE", "10000");
            relatedAudits.forEach(related -> {
                var ip = related.findIp();
                var userAgent = related.getUserAgent();
                var cookie = related.getCookie();
                if (related.isTor()) {
                    score.setPlain(score.getPlain() + torScore);
                }
                addIfExisting(ips, ip);
                addIfExisting(userAgents, userAgent);
                addIfExisting(cookies, cookie);
            });
            var ipsScore = configurationBo.findIntOrSetDefault("MTAS_IP_SCORE", "5000");
            var uaScore = configurationBo.findIntOrSetDefault("MTAS_UA_SCORE", "1");
            var cookieScore = configurationBo.findIntOrSetDefault("MTAS_COOKIE_SCORE", "10000");
            ips.forEach((key, value) -> score.setPlain(score.getPlain() + value.getPlain() * ipsScore));
            userAgents.forEach((key, value) -> score.setPlain(score.getPlain() + value.getPlain() * uaScore));
            cookies.forEach((key, value) -> score.setPlain(score.getPlain() + value.getPlain() * cookieScore));
            user.setMultiAccountScore((float) score.getPlain());
            user.setLastMultiAccountCheck(LocalDateTime.now());
            checkShouldBan(user);
            userStorageBo.save(user);
        });
    }

    private void handleMyData(List<AuditDataProjection> data, Map<String, AtomicInteger> ips, Map<String, AtomicInteger> uas, Map<String, AtomicInteger> cookies) {
        data.forEach(current -> {
            handleMap(ips, current.getIp());
            handleMap(uas, current.getUserAgent());
            handleMap(cookies, current.getCookie());
        });
    }

    private void addIfExisting(Map<String, AtomicInteger> targetMap, String key) {
        if (key != null && targetMap.containsKey(key)) {
            targetMap.get(key).incrementAndGet();
        }
    }

    private void handleMap(Map<String, AtomicInteger> map, String key) {
        if (key != null) {
            if (map.containsKey(key)) {
                map.get(key).incrementAndGet();
            } else {
                map.put(key, new AtomicInteger(0));
            }
        }
    }

    private void checkShouldBan(UserStorage user) {
        var warnScore = configurationBo.findIntOrSetDefault("MTAS_WARN_SCORE", "100000");
        var banScore = configurationBo.findIntOrSetDefault("MTAS_BAN_SCORE", "200000");
        var userScore = user.getMultiAccountScore();
        if (userScore > banScore) {
            user.setBanned(true);
        } else {
            user.setBanned(false);
            if (userScore > warnScore) {
                socketIoService.sendWarning(user, "I18N_WARN_MULTI_ACCOUNT");
            }
        }
    }
}
