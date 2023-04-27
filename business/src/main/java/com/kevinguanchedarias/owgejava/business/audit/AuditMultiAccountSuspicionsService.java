package com.kevinguanchedarias.owgejava.business.audit;

import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.dto.SuspicionDto;
import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.Suspicion;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.SuspicionSourceEnum;
import com.kevinguanchedarias.owgejava.repository.AuditRepository;
import com.kevinguanchedarias.owgejava.repository.SuspicionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class AuditMultiAccountSuspicionsService implements UserDeleteListener {
    public static final long WANTED_MS_DELAY = 1000;
    private static final long MAX_BACK_LOOKUP_SECONDS = 86400L * 7L;
    private final AsyncRunnerBo asyncRunnerBo;
    private final AuditRepository auditRepository;
    private final SuspicionRepository suspicionRepository;

    public void handle(Audit audit) {
        if (audit.findIp() != null || audit.getCookie() != null) {
            asyncRunnerBo.runAsyncWithoutContextDelayed(() -> doHandle(audit), WANTED_MS_DELAY, Thread.MIN_PRIORITY);
        }
    }

    public List<SuspicionDto> findLast100() {
        return suspicionRepository.findAll(
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).stream().map(SuspicionDto::of).toList();
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        suspicionRepository.deleteByRelatedUser(user);
    }

    private void doHandle(Audit audit) {
        var user = audit.getUser();
        log.debug("Handling suspicious for user {}({})", user.getUsername(), user.getId());
        var maxBackLookupDate = LocalDateTime.now().minusSeconds(MAX_BACK_LOOKUP_SECONDS);
        auditRepository.findSuspicions(
                        maxBackLookupDate, user, audit.getCookie(), audit.getIpv4(), audit.getIpv6()
                ).stream()
                .filter(lookingAudit -> !suspicionRepository.existsByRelatedUserAndRelatedAudit(user, lookingAudit))
                .forEach(lookingAudit -> doAddNewSuspicion(user, audit, lookingAudit));
    }

    private void doAddNewSuspicion(UserStorage user, Audit triggeringAudit, Audit lookingAudit) {
        var isSameIp = isSameString(triggeringAudit.findIp(), lookingAudit.findIp());
        var isSameCookie = isSameString(triggeringAudit.getCookie(), lookingAudit.getCookie());
        var source = determineSource(isSameIp, isSameCookie);
        suspicionRepository.save(Suspicion.builder()
                .source(source)
                .relatedUser(user)
                .relatedAudit(lookingAudit)
                .createdAt(LocalDateTime.now())
                .build()
        );
    }

    private SuspicionSourceEnum determineSource(boolean isSameIp, boolean isSameCookie) {
        if (isSameIp && isSameCookie) {
            return SuspicionSourceEnum.BROWSER_AND_IP;
        } else if (isSameIp) {
            return SuspicionSourceEnum.IP;
        } else {
            return SuspicionSourceEnum.BROWSER;
        }
    }

    private boolean isSameString(String ip, String ip2) {
        return ip != null && ip.equals(ip2);
    }
}
