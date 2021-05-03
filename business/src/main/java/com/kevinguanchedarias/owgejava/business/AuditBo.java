package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.AuditDto;
import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.projection.AuditDataProjection;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AuditRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Log4j2
public class AuditBo implements BaseBo<Long, Audit, AuditDto> {
    private static final String CONTROL_COOKIE_NAME = "OWGE_TMAS";
    private static final int DAYS = 365;

    private final transient AuditRepository repository;
    private final UserStorageBo userStorageBo;
    private final transient TorClientBo torClientBo;
    private final transient AsyncRunnerBo asyncRunnerBo;
    private final transient  SocketIoService socketIoService;

    @Override
    public JpaRepository<Audit, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<AuditDto> getDtoClass() {
        return AuditDto.class;
    }

    /**
     * @todo in the future use the httpOnly, now unused as Kevinsuite depends on servlet 2.5, and httpOnly was introduced in 3.0
     */
    public void creteCookieIfMissing(HttpServletRequest request, HttpServletResponse response) {
        if (WebUtils.getCookie(request, AuditBo.CONTROL_COOKIE_NAME) == null) {
            var cookie = new Cookie(AuditBo.CONTROL_COOKIE_NAME, String.valueOf(RandomUtils.nextDouble(0d, 100000d)));
            cookie.setMaxAge(DAYS * 7 * 24 * 60 * 60);
            cookie.setPath("/game_api");
            response.addCookie(cookie);
        }
    }

    /**
     * Finds the nearest Audit (<b>With request</b>) of one user
     */
    public Optional<Audit> findNearest(LocalDateTime date, Integer userId) {
        var list = findNearestPage(date, userId, 1);
        return list.isEmpty()
                ? Optional.empty()
                : Optional.of(list.get(0));
    }

    public List<Audit> findNearestPage(LocalDateTime date, Integer userId, int count) {
        return repository.findNearesRequestAction(date, userId, PageRequest.of(0, count));
    }

    public List<AuditDataProjection> findDistinctData(Integer userId) {
        var now = LocalDateTime.now();
        return repository.findDistinctByUserIdAndCreationDateBetween(userId, now.minus(15, ChronoUnit.DAYS), now, PageRequest.of(0, 100));
    }

    public void nonRequestAudit(AuditActionEnum action, String actionDetails, UserStorage user, Integer relatedUser) {
        var now = LocalDateTime.now();
        var nearest = findNearest(now, user.getId());
        String ip = null;
        String ua = null;
        String cookie = null;
        if(nearest.isPresent()) {
            var nearestAudit = nearest.get();
            ip = nearestAudit.getIp();
            ua = nearestAudit.getUserAgent();
            cookie = nearestAudit.getCookie();
        }
        repository.save(Audit.builder()
                .action(action)
                .actionDetail(actionDetails)
                .ip(ip)
                .userAgent(ua)
                .cookie(cookie)
                .user(user)
                .relatedUser(userStorageBo.getOne(relatedUser))
                .creationDate(now)
                .build()
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void doAudit(AuditActionEnum action) {
        doAudit(action, null, null);
    }

    /**
     * <b>NOTICE:</b> Has to be executed in the scope of an http request
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void doAudit(AuditActionEnum action, String actionDetails, Integer relatedUserId) {
        var requestAttributes = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
        if(requestAttributes == null) {
            throw new ProgrammingException("Using doAudit outside of request thread");
        } else {
            var request = requestAttributes.getRequest();
            var cookie = WebUtils.getCookie(request, CONTROL_COOKIE_NAME);
            if (cookie == null) {
                throw new SgtBackendInvalidInputException("No dear hacker, you will never be able to defeat the strong security of this open security-by-obscurity");
            }
            detectTorBrowser(
                repository.save(Audit.builder()
                    .action(action)
                    .actionDetail(actionDetails)
                    .user(userStorageBo.getOne(userStorageBo.findLoggedIn().getId()))
                    .relatedUser(userStorageBo.getOne(relatedUserId))
                    .ip(request.getRemoteAddr())
                    .userAgent(request.getHeader("User-Agent"))
                    .cookie(cookie.getValue())
                    .creationDate(LocalDateTime.now())
                    .build()
                )
            );
        }
    }


    public List<Audit> findRelated(UserStorage user) {
        return repository.findByRelatedUser(user);
    }

    private void detectTorBrowser(Audit audit) {
        asyncRunnerBo.runAssyncWithoutContextDelayed(() -> {
            var ip = audit.getIp();
            try {
                var inetAddress = InetAddress.getByName(ip);
                var host = inetAddress.getHostName();
                if (!inetAddress.isLoopbackAddress() && !inetAddress.isSiteLocalAddress() && (host.contains("tor-exit") || isInTorList(ip))) {
                    audit.setTor(true);
                    socketIoService.sendWarning(audit.getUser(), "I18N_WARN_TOR");
                }
            } catch (UnknownHostException e) {
                log.trace("Can't resolve name for ip {}", ip);
            }
        }, 3000);
    }

    private boolean isInTorList(String ip) {
        return torClientBo.isTor(ip);
    }

}
