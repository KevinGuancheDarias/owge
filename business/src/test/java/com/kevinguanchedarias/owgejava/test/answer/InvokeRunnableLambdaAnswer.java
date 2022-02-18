package com.kevinguanchedarias.owgejava.test.answer;

import lombok.AllArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@AllArgsConstructor
public class InvokeRunnableLambdaAnswer implements Answer<Void> {

    private final int argumentPosition;

    @Override
    public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        invocationOnMock.getArgument(argumentPosition, Runnable.class).run();
        return null;
    }
}
