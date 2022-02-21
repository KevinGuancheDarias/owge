package com.kevinguanchedarias.owgejava.test.answer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class InvokeConsumerLambdaAnswer<T> implements Answer<T> {
    private final int position;

    @Getter
    private Consumer<T> passedLambda;

    @SuppressWarnings("unchecked")
    @Override
    public T answer(InvocationOnMock invocationOnMock) {
        passedLambda = invocationOnMock.getArgument(position, Consumer.class);
        return null;
    }
}
