package com.kevinguanchedarias.owgejava.enumerations;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DocTypeEnumTest {

    @ParameterizedTest
    @CsvSource({
            "EXCEPTIONS,exceptions",
            "RESERVED,reserved"
    })
    void findPath_should_work(DocTypeEnum docTypeEnum, String expectation) {
        assertThat(docTypeEnum.findPath()).isEqualTo(expectation);
    }
}
