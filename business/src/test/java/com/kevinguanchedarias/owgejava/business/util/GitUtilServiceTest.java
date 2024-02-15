package com.kevinguanchedarias.owgejava.business.util;

import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.util.GitUtilService;
import com.kevinguanchedarias.owgejava.util.MavenUtilService;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = GitUtilService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(MavenUtilService.class)
@AllArgsConstructor(onConstructor_ = @Autowired)
class GitUtilServiceTest {
    private final GitUtilService gitUtilService;
    private final MavenUtilService mavenUtilService;

    @Test
    void createDocUrl_should_throw() {
        assertThatThrownBy(() -> gitUtilService.createDocUrl(GameProjectsEnum.BUSINESS, Object.class, DocTypeEnum.EXCEPTIONS, "FOO"))
                .isInstanceOf(ProgrammingException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "EXCEPTIONS, I18N_ERR_FOO,master,https://github.com/KevinGuancheDarias/owge/blob/master/business/docs/java.lang.Object/exceptions/i18n_err_foo.md",
            "EXCEPTIONS, I18N_ERR_FOO,0.11.4,https://github.com/KevinGuancheDarias/owge/blob/v0.11.4/business/docs/java.lang.Object/exceptions/i18n_err_foo.md",
            "RESERVED,FOO,master,https://github.com/KevinGuancheDarias/owge/blob/master/business/docs/java.lang.Object/reserved/foo.md"
    })
    void createDocUrl_should_work(DocTypeEnum docTypeEnum, String doc, String version, String expectation) {
        given(mavenUtilService.findVersion("master")).willReturn(version);
        assertThat(gitUtilService.createDocUrl(GameProjectsEnum.BUSINESS, Object.class, docTypeEnum, doc)).isEqualTo(expectation);
    }
}
