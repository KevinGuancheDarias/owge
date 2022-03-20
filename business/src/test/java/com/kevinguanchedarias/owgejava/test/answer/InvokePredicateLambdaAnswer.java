package com.kevinguanchedarias.owgejava.test.answer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.function.Predicate;

@RequiredArgsConstructor
public class InvokePredicateLambdaAnswer<T> implements Answer<Boolean> {
    private final int position;
    private final T lambdaParam;

    @Getter
    private Predicate<T> passedLambda;

    @SuppressWarnings("unchecked")
    @Override
    public Boolean answer(InvocationOnMock invocationOnMock) {
        passedLambda = invocationOnMock.getArgument(position, Predicate.class);
        return passedLambda.test(lambdaParam);
    }
}
