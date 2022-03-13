package com.kevinguanchedarias.owgejava.test.answer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class InvokeRunnableLambdaAnswer implements Answer<Void> {

    private final int argumentPosition;

    @Getter
    private final List<Runnable> runnableList = new ArrayList<>();

    @Override
    public Void answer(InvocationOnMock invocationOnMock) {
        var runnable = invocationOnMock.getArgument(argumentPosition, Runnable.class);
        runnable.run();
        runnableList.add(runnable);
        return null;
    }
}
