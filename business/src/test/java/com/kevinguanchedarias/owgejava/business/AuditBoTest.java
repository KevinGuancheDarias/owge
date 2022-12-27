package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AuditRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.AuditMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = AuditBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        AuditRepository.class,
        UserSessionService.class,
        TorClientBo.class,
        AsyncRunnerBo.class,
        SocketIoService.class,
        UserStorageRepository.class
})
class AuditBoTest {
    private final AuditBo auditBo;
    private final AuditRepository repository;
    private final UserSessionService userSessionService;
    private final UserStorageRepository userStorageRepository;

    @Autowired
    AuditBoTest(
            AuditBo auditBo,
            AuditRepository repository,
            UserSessionService userSessionService,
            UserStorageRepository userStorageRepository
    ) {
        this.auditBo = auditBo;
        this.repository = repository;
        this.userSessionService = userSessionService;
        this.userStorageRepository = userStorageRepository;
    }

    @Test
    void nonRequestAudit_should_work_with_nearest_info_if_present() {
        var nearestAudit = givenAudit();
        given(repository.findNearesRequestAction(any(), eq(USER_ID_1), any())).willReturn(List.of(nearestAudit));
        var user = givenUser1();
        var relatedUser = givenUser2();
        var actionDetail = "foo";
        given(userStorageRepository.getReferenceById(USER_ID_2)).willReturn(relatedUser);

        auditBo.nonRequestAudit(AuditActionEnum.REGISTER_MISSION, actionDetail, user, USER_ID_2);

        var captor = ArgumentCaptor.forClass(Audit.class);
        verify(repository, times(1)).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditActionEnum.REGISTER_MISSION);
        assertThat(saved.getActionDetail()).isEqualTo(actionDetail);
        assertThat(saved.getIp()).isEqualTo(AUDIT_IP);
        assertThat(saved.getUserAgent()).isEqualTo(AUDIT_USER_AGENT);
        assertThat(saved.getCookie()).isEqualTo(AUDIT_COOKIE);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getRelatedUser()).isSameAs(relatedUser);
        assertThat(saved.getCreationDate()).isBetween(LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
    }

    @Test
    void nonRequestAudit_should_save_with_nulls_when_no_nearest_request_is_present() {
        var user = givenUser1();

        auditBo.nonRequestAudit(AuditActionEnum.REGISTER_MISSION, null, user, null);

        var captor = ArgumentCaptor.forClass(Audit.class);
        verify(repository, times(1)).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getIp()).isNull();
        assertThat(saved.getUserAgent()).isNull();
        assertThat(saved.getCookie()).isNull();
        assertThat(saved.getRelatedUser()).isNull();
        verify(userStorageRepository, never()).getReferenceById(any());
    }

    @Test
    void doAudit_should_throw_when_no_request_attributes() {
        assertThatThrownBy(() -> auditBo.doAudit(AuditActionEnum.REGISTER_MISSION))
                .isInstanceOf(ProgrammingException.class)
                .hasMessageContaining("outside of request");
    }

    @Test
    void doAudit_should_throw_when_no_cookie() {
        var requestAttributes = mock(ServletRequestAttributes.class);
        var request = mock(HttpServletRequest.class);
        given(requestAttributes.getRequest()).willReturn(request);
        try (var mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);

            assertThatThrownBy(() -> auditBo.doAudit(AuditActionEnum.REGISTER_MISSION))
                    .isInstanceOf(SgtBackendInvalidInputException.class)
                    .hasMessageContaining("No dear hacker");
        }
    }

    @ParameterizedTest
    @MethodSource("doAudit_should_work_arguments")
    void doAudit_should_work(Integer relatedUser, UserStorage expectedRelatedUser, int timesGetReference) {
        var requestAttributes = mock(ServletRequestAttributes.class);
        var request = mock(HttpServletRequest.class);
        var cookie = mock(Cookie.class);
        var cookieValue = "FooCookieValue";
        var user = givenUser1();
        var proxyIp = "192.168.0.1";
        given(requestAttributes.getRequest()).willReturn(request);
        given(cookie.getValue()).willReturn(cookieValue);
        given(request.getHeader("User-Agent")).willReturn(AUDIT_USER_AGENT);
        given(request.getRemoteAddr()).willReturn(proxyIp);
        given(request.getHeader("X-OWGE-RMT-IP")).willReturn(AUDIT_IP);
        given(userSessionService.findLoggedInWithReference()).willReturn(user);
        given(userStorageRepository.getReferenceById(relatedUser)).willReturn(expectedRelatedUser);

        try (
                var requestContextHolderMockedStatic = mockStatic(RequestContextHolder.class);
                var webUtilsMockedStatic = mockStatic(WebUtils.class)
        ) {
            requestContextHolderMockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
            webUtilsMockedStatic.when(() -> WebUtils.getCookie(request, AuditBo.CONTROL_COOKIE_NAME)).thenReturn(cookie);

            auditBo.doAudit(AuditActionEnum.REGISTER_MISSION, null, relatedUser);

            var captor = ArgumentCaptor.forClass(Audit.class);
            verify(repository, times(1)).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getAction()).isEqualTo(AuditActionEnum.REGISTER_MISSION);
            assertThat(saved.getActionDetail()).isNull();
            assertThat(saved.getIp()).isEqualTo(AUDIT_IP);
            assertThat(saved.getUserAgent()).isEqualTo(AUDIT_USER_AGENT);
            assertThat(saved.getCookie()).isEqualTo(cookieValue);
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getRelatedUser()).isEqualTo(expectedRelatedUser);
            assertThat(saved.getCreationDate()).isBetween(LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(10));
            verify(userStorageRepository, times(timesGetReference)).getReferenceById(relatedUser);
        }
    }

    private static Stream<Arguments> doAudit_should_work_arguments() {
        return Stream.of(
                Arguments.of(null, null, 0),
                Arguments.of(USER_ID_2, givenUser2(), 1)
        );
    }
}
