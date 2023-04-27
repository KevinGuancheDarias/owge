package com.kevinguanchedarias.owgejava.test.answer;

import lombok.AllArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@AllArgsConstructor
public class NotReturningAnswer implements Answer<Void> {
    private final Runnable action;

    @Override
    public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        action.run();
        return null;
    }
}
