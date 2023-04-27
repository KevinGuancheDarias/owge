package com.kevinguanchedarias.owgejava.test.matcher;

import lombok.AllArgsConstructor;
import org.mockito.ArgumentMatcher;

import java.util.List;

@AllArgsConstructor
public class ListContainsMatcher<E> implements ArgumentMatcher<List<E>> {
    private final List<E> wantedValues;

    @Override
    public boolean matches(List<E> invocationArgument) {
        return !invocationArgument.isEmpty() && invocationArgument.containsAll(wantedValues);
    }
}
