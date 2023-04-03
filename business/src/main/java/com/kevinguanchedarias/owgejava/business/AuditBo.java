package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.dto.AuditDto;
import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.projection.AuditDataProjection;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AuditRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Value;
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
import java.io.Serial;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuditBo implements BaseBo<Long, Audit, AuditDto> {
    @Serial
    private static final long serialVersionUID = -890947636309038855L;

    public static final String CONTROL_COOKIE_NAME = "OWGE_TMAS";

    private static final int DAYS = 365;
    private static final String TRUSTED_PRIVATE_NET_KEYWORD = "PRIVATE";

    private final transient AuditRepository repository;
    private final transient UserSessionService userSessionService;
    private final transient TorClientBo torClientBo;
    private final transient AsyncRunnerBo asyncRunnerBo;
    private final transient SocketIoService socketIoService;
    private final UserStorageRepository userStorageRepository;

    @Value("${OWGE_PROXY_TRUSTED_NETWORKS:PRIVATE}")
    private String proxyTrustedNetworks;

    @Value("${OWGE_PROXY_TRUSTED_HEADER:X-OWGE-RMT-IP}")
    private String proxyTrustedHeader;

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
        String ipv4 = null;
        String ipv6 = null;
        String ua = null;
        String cookie = null;
        if (nearest.isPresent()) {
            var nearestAudit = nearest.get();
            ipv4 = nearestAudit.getIpv4();
            ipv6 = nearestAudit.getIpv6();
            ua = nearestAudit.getUserAgent();
            cookie = nearestAudit.getCookie();
        }
        repository.save(Audit.builder()
                .action(action)
                .actionDetail(actionDetails)
                .ipv4(ipv4)
                .ipv6(ipv6)
                .userAgent(ua)
                .cookie(cookie)
                .user(user)
                .relatedUser(relatedUser == null ? null : userStorageRepository.getReferenceById(relatedUser))
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
        if (requestAttributes == null) {
            throw new ProgrammingException("Using doAudit outside of request thread");
        } else {
            var request = requestAttributes.getRequest();
            var cookie = WebUtils.getCookie(request, CONTROL_COOKIE_NAME);
            if (cookie == null) {
                throw new SgtBackendInvalidInputException("No dear hacker, you will never be able to defeat the strong security of this open security-by-obscurity");
            }
            detectTorBrowser(request,
                    repository.save(Audit.builder()
                            .action(action)
                            .actionDetail(actionDetails)
                            .user(userSessionService.findLoggedInWithReference())
                            .relatedUser(relatedUserId == null ? null : userStorageRepository.getReferenceById(relatedUserId))
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

    private String resolveIp(HttpServletRequest request) {
        var ip = request.getRemoteAddr();
        if (proxyTrustedHeader.isEmpty() || !isTrustedProxyIp(ip)) {
            return ip;
        } else {
            return request.getHeader(proxyTrustedHeader);
        }
    }


    private boolean isTrustedProxyIp(String ip) {
        return Stream.of(proxyTrustedNetworks.split(",")).anyMatch(trusted ->
                (trusted.equals(TRUSTED_PRIVATE_NET_KEYWORD) && isPrivate(ip))
                        || trusted.equals(ip)
        );
    }

    private void detectTorBrowser(HttpServletRequest request, Audit audit) {
        var ip = resolveIp(request);
        asyncRunnerBo.runAssyncWithoutContextDelayed(() -> {
            try {
                var inetAddress = InetAddress.getByName(ip);
                var host = inetAddress.getHostName();
                if (!isPrivate(inetAddress) && (host.contains("tor-exit") || isInTorList(ip))) {
                    audit.setTor(true);
                    socketIoService.sendWarning(audit.getUser(), "I18N_WARN_TOR");
                }
                maybeSetIpv4OrIpv6(audit, inetAddress, ip);
            } catch (UnknownHostException e) {
                log.warn("Can't resolve name for ip {}", ip);
            }
        }, 3000);
    }

    private void maybeSetIpv4OrIpv6(Audit audit, InetAddress inetAddress, String ip) {
        if (inetAddress instanceof Inet4Address) {
            audit.setIpv4(ip);
        } else {
            audit.setIpv6(ip);
        }
    }

    private boolean isPrivate(String ip) {
        try {
            return isPrivate(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            log.warn("Passed argument is not an IP, passed: {} ", ip);
            return false;
        }

    }

    private boolean isPrivate(InetAddress inetAddress) {
        return inetAddress.isLoopbackAddress() || inetAddress.isSiteLocalAddress();
    }

    private boolean isInTorList(String ip) {
        return torClientBo.isTor(ip);
    }

}
