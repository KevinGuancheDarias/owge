package com.kevinguanchedarias.owgejava.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.owgejava.GlobalConstants;
import com.kevinguanchedarias.owgejava.annotation.WebControllerCache;
import com.kevinguanchedarias.owgejava.util.filter.ArgumentResolutionInvocableHandlerMethod;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Will manage the cache for unauthenticated websocket-sync requests
 */
@Component
@Order(GlobalConstants.OPEN_WEBSOCKET_SYNC_FILTER)
@RequiredArgsConstructor
@Slf4j
public class OpenWebsocketSyncFilter implements Filter {

    private final TaggableCacheManager taggableCacheManager;
    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerAdapter handlerAdapter;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private record CacheInfo(LocalDateTime lastModified, String body) {
    }


    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        Optional.ofNullable(handlerMapping.getHandler((HttpServletRequest) servletRequest)
                ).map(HandlerExecutionChain::getHandler)
                .filter(HandlerMethod.class::isInstance)
                .map(HandlerMethod.class::cast)
                .filter(this::methodHasAnnotation)
                .ifPresentOrElse(
                        handlerMethod -> doHandle(handlerMethod, servletRequest, servletResponse),
                        () -> this.doFilter(filterChain, servletRequest, servletResponse)
                );
    }

    @SneakyThrows
    private void doFilter(FilterChain filterChain, ServletRequest request, ServletResponse response) {
        filterChain.doFilter(request, response);
    }

    private boolean methodHasAnnotation(HandlerMethod handlerMethod) {
        var annotation = handlerMethod.getMethodAnnotation(WebControllerCache.class);
        return annotation != null
                && CollectionUtils.isNotEmpty(List.of(annotation.tags()));
    }

    @SneakyThrows
    private void doHandle(HandlerMethod handlerMethod, ServletRequest servletRequest, ServletResponse servletResponse) {
        var annotation = handlerMethod.getMethodAnnotation(WebControllerCache.class);
        var webRequest = new ServletWebRequest((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
        var invocableMethod = getInvocableMethod(handlerMethod, handlerAdapter);
        var params = invocableMethod.resolveArguments(webRequest);
        var cacheKey = buildCacheKey(handlerMethod, params);
        var httpServletRequest = (HttpServletRequest) servletRequest;
        var httpServletResponse = (HttpServletResponse) servletResponse;
        if (taggableCacheManager.keyExists(cacheKey)) {
            CacheInfo cacheInfo = taggableCacheManager.findByKey(cacheKey);
            log.debug("Sending cached value for {}", cacheKey);
            addCacheHeaders(httpServletResponse, cacheInfo);

            maybeSendBody(httpServletRequest, servletResponse, cacheInfo);
        } else {
            assert annotation != null;
            log.debug("Computing value for {}", cacheKey);
            Object body = invocableMethod.invokeForRequest(webRequest, null);
            if (body != null) {
                var jsonEncodedContent = objectMapper.writeValueAsString(body);
                var cacheInfo = new CacheInfo(LocalDateTime.now(), jsonEncodedContent);
                addCacheHeaders(httpServletResponse, cacheInfo);
                maybeSendBody(httpServletRequest, httpServletResponse, cacheInfo);
                taggableCacheManager.saveEntry(cacheKey, cacheInfo, List.of(annotation.tags()));
            }
        }
    }

    @SneakyThrows
    private void maybeSendBody(HttpServletRequest request, ServletResponse servletResponse, CacheInfo cacheInfo) {
        var httpServletResponse = (HttpServletResponse) servletResponse;
        var header = request.getHeader("If-Modified-Since");
        if (header != null && (
                cacheInfo.lastModified.equals(LocalDateTime.parse(header))
                        || cacheInfo.lastModified.isBefore(LocalDateTime.parse(header)))) {
            httpServletResponse.setStatus(HttpStatus.NOT_MODIFIED.value());
        } else {
            servletResponse.getWriter().append(cacheInfo.body);
        }
    }

    private void addCacheHeaders(HttpServletResponse response, CacheInfo cacheInfo) {
        response.addHeader("Cache-Control", "must-revalidate");
        response.addHeader("Last-Modified", cacheInfo.lastModified.toString());
    }

    private String buildCacheKey(HandlerMethod handlerMethod, Object[] params) {
        return handlerMethod.getBean() + handlerMethod.getMethod().getName() + Arrays.toString(params);
    }

    private ArgumentResolutionInvocableHandlerMethod getInvocableMethod(HandlerMethod handlerMethod, RequestMappingHandlerAdapter handlerAdapter) {
        var invocableHandlerMethod = new ArgumentResolutionInvocableHandlerMethod(handlerMethod);
        var argumentResolvers = new HandlerMethodArgumentResolverComposite();
        argumentResolvers.addResolvers(handlerAdapter.getArgumentResolvers());
        invocableHandlerMethod.setHandlerMethodArgumentResolvers(argumentResolvers);
        return invocableHandlerMethod;
    }
}
