package com.kevinguanchedarias.owgejava.filter;

import com.kevinguanchedarias.owgejava.business.AuthenticationBo;
import com.kevinguanchedarias.owgejava.context.OwgeContextHolder;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import io.opentelemetry.api.trace.Span;
import lombok.AllArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

@Component
@AllArgsConstructor
@Order
public class OwgeContextFilter implements Filter {
    private final AuthenticationBo authenticationBo;
    private final PlanetRepository planetRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpServletRequest) {
            var token = authenticationBo.findTokenUser();
            if (token != null) {
                Span.current().setAttribute("kw.user_id", (Integer) token.getId());
                Span.current().setAttribute("kw.username", token.getUsername());
                loadContext(httpServletRequest);
            }
        }
        chain.doFilter(request, response);
        OwgeContextHolder.clear();
    }

    private void loadContext(HttpServletRequest request) {
        Optional.ofNullable(request.getHeader("X-OWGE-Selected-Planet"))
                .map(Long::parseLong)
                .ifPresent(planetId -> OwgeContextHolder.set(new OwgeContextHolder.OwgeContext(maybeSetPlanet(planetId), null)));
    }

    private Long maybeSetPlanet(Long wantedPlanetId) {
        var userId = authenticationBo.findTokenUser().getId().intValue();
        return planetRepository.findOneByIdAndOwnerId(wantedPlanetId, userId)
                .map(Planet::getId)
                .orElse(planetRepository.findOneByOwnerIdAndHomeTrue(userId).getId());
    }
}
