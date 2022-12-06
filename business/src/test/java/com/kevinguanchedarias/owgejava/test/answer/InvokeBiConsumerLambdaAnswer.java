package com.kevinguanchedarias.owgejava.test.answer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class InvokeBiConsumerLambdaAnswer<T, U> implements Answer<T> {
    private final int position;

    @Getter
    private BiConsumer<T, U> passedLambda;

    @SuppressWarnings("unchecked")
    @Override
    public T answer(InvocationOnMock invocationOnMock) {
        passedLambda = invocationOnMock.getArgument(position, BiConsumer.class);
        return null;
    }
}
