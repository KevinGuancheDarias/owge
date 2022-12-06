package com.kevinguanchedarias.owgejava.util;

import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class SpringRepositoryUtilTest {

    @Test
    void findEntityClass_should_properly_handle_mockito(CapturedOutput capturedOutput) {
        assertThat(SpringRepositoryUtil.findEntityClass(mock(JpaRepository.class))).isNull();
        assertThat(capturedOutput.getOut()).contains("Inside mockito");
    }

    @Test
    void findEntityClass_should_throw_on_non_proxy() {
        assertThatThrownBy(() -> SpringRepositoryUtil.findEntityClass("FOO"))
                .isInstanceOf(ProgrammingException.class)
                .hasMessageContaining("a repository proxy instance");
    }

    @Test
    void existsOrDie_should_throw_not_found() {
        assertThatThrownBy(() ->
                SpringRepositoryUtil.existsOrDie(mock(UnitRepository.class), 1)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void existsOrDie_should_work() {
        var repositoryMock = mock(UnitRepository.class);
        given(repositoryMock.existsById(UNIT_ID_1)).willReturn(true);

        SpringRepositoryUtil.existsOrDie(repositoryMock, UNIT_ID_1);

        verify(repositoryMock, times(1)).existsById(UNIT_ID_1);
    }
}
