package com.kevinguanchedarias.owgejava.test.util;

import com.kevinguanchedarias.owgejava.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationUtilTestHelper {
    private final ValidationUtil mockedInstance;

    public static ValidationUtilTestHelper getInstance(MockedStatic<ValidationUtil> validationUtilMockedStatic) {
        var mockedInstance = mock(ValidationUtil.class);
        validationUtilMockedStatic.when(ValidationUtil::getInstance).thenReturn(mockedInstance);
        var instance = new ValidationUtilTestHelper(mockedInstance);
        instance.configureMockAnswers();
        return instance;
    }

    public ValidationUtilTestHelper assertRequireNotNull(Object value, String position) {
        verify(mockedInstance, times(1)).requireNotNull(value, position);
        return this;
    }

    public ValidationUtilTestHelper assertRequireNull(String position) {
        verify(mockedInstance, times(1)).requireNull(isNull(), eq(position));
        return this;
    }

    public <E extends Enum<E>> ValidationUtilTestHelper assertRequireValidEnumValue(String value, Class<E> targetEnum, String position) {
        verify(mockedInstance, times(1)).requireValidEnumValue(value, targetEnum, position);
        return this;
    }

    public ValidationUtilTestHelper assertRequirePositiveNumber(Integer value, String position) {
        verify(mockedInstance, times(1)).requirePositiveNumber(value, position);
        return this;
    }

    public ValidationUtilTestHelper assertRequirePositiveNumber(Long value, String position) {
        verify(mockedInstance, times(1)).requirePositiveNumber(value, position);
        return this;
    }

    private void configureMockAnswers() {
        when(mockedInstance.requireNotNull(any(), any())).thenReturn(mockedInstance);
        when(mockedInstance.requireNull(any(), any())).thenReturn(mockedInstance);
        when(mockedInstance.requireValidEnumValue(any(), any(), any())).thenReturn(mockedInstance);
        when(mockedInstance.requirePositiveNumber(anyInt(), any())).thenReturn(mockedInstance);
        when(mockedInstance.requirePositiveNumber(anyLong(), any())).thenReturn(mockedInstance);
    }
}
