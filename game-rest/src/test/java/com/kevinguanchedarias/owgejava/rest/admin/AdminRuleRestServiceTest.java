package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.OwgeTestConfiguration;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleItemTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.kevinguanchedarias.owgejava.RestTestUtils.restGiven;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.ORIGIN_ID;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.ORIGIN_TYPE;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.TYPE;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleDto;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleItemTypeDescriptor;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleTypeDescriptorDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = AdminRuleRestService.class)
@Import(OwgeTestConfiguration.class)
@MockBean({
        RuleBo.class
})
class AdminRuleRestServiceTest {
    private static final String BASE_PATH = "admin/rules";

    private final RuleBo ruleBo;

    @Autowired
    public AdminRuleRestServiceTest(RuleBo ruleBo, WebApplicationContext webApplicationContext) {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        this.ruleBo = ruleBo;
    }

    @Test
    void findByOriginTypeAndOriginId_should_work() {
        var dto = givenRuleDto();
        when(ruleBo.findByOriginTypeAndOriginId(ORIGIN_TYPE, ORIGIN_ID)).thenReturn(List.of(dto));

        var response = restGiven()
                .when()
                .get(BASE_PATH + "/origin/{originType}/{id}", ORIGIN_TYPE, ORIGIN_ID)

                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        var retVal = response.getBody().jsonPath().getList("", RuleDto.class);
        assertThat(retVal).hasSize(1);
        var returned = retVal.get(0);
        assertEquals(dto, returned);
    }

    @Test
    void findByType_should_work() {
        var dto = givenRuleDto();
        when(ruleBo.findByType(TYPE)).thenReturn(List.of(dto));

        var response = restGiven()
                .when()
                .get(BASE_PATH + "/type/{type}", TYPE)

                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        var retVal = response.getBody().jsonPath().getList("", RuleDto.class);
        assertThat(retVal).hasSize(1);
        var returned = retVal.get(0);
        assertEquals(dto, returned);
    }

    @Test
    void findRuleTypeDescriptor_should_work() {
        var dto = givenRuleTypeDescriptorDto();
        when(ruleBo.findTypeDescriptor(TYPE)).thenReturn(dto);

        var response = restGiven()
                .when()
                .get(BASE_PATH + "/type-descriptor/{type}", TYPE)

                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        verify(ruleBo).findTypeDescriptor(TYPE);
        var retVal = response.getBody().as(RuleTypeDescriptorDto.class);
        assertThat(retVal)
                .isNotNull()
                .isEqualTo(dto);
    }

    @Test
    void findRuleItemTypeDescriptor_should_work() {
        var dto = givenRuleItemTypeDescriptor();
        when(ruleBo.findItemTypeDescriptor(TYPE)).thenReturn(dto);

        var response = restGiven()
                .when()
                .get(BASE_PATH + "/item-type-descriptor/{type}", TYPE)

                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        verify(ruleBo).findItemTypeDescriptor(TYPE);
        var retVal = response.getBody().as(RuleItemTypeDescriptorDto.class);
        assertThat(retVal)
                .isNotNull()
                .isEqualTo(dto);
    }

    @Test
    void deleteById_should_work() {
        int id = 8;
        restGiven()
                .when()
                .delete(BASE_PATH + "/{id}", id)

                .then()
                .log().ifValidationFails()
                .statusCode(OK.value());

        verify(ruleBo, times(1)).deleteById(id);
    }

    @Test
    void save_should_work() {
        var dto = givenRuleDto();
        when(this.ruleBo.save(dto)).thenReturn(dto);

        var response = restGiven()
                .body(dto)
                .when()
                .post(BASE_PATH)

                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        var retVal = response.getBody().as(RuleDto.class);
        assertEquals(dto, retVal);
        verify(this.ruleBo, times(1)).save(dto);
    }
}
