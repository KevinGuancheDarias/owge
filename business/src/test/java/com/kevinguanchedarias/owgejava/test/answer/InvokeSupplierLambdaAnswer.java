package com.kevinguanchedarias.owgejava.test.answer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class InvokeSupplierLambdaAnswer<T> implements Answer<T> {
    private final int position;
    
    @Getter
    private T result;

    @SuppressWarnings("unchecked")
    @Override
    public T answer(InvocationOnMock invocationOnMock) throws Throwable {
        result = (T) invocationOnMock.getArgument(position, Supplier.class).get();
        return result;
    }
}
