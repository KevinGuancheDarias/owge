package com.kevinguanchedarias.owgejava.util.filter;

import lombok.SneakyThrows;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;

public class ArgumentResolutionInvocableHandlerMethod extends InvocableHandlerMethod {

    public ArgumentResolutionInvocableHandlerMethod(HandlerMethod handlerMethod) {
        super(handlerMethod);
    }

    @SneakyThrows
    public Object[] resolveArguments(NativeWebRequest webRequest) {
        return getMethodArgumentValues(webRequest, null);
    }
}
