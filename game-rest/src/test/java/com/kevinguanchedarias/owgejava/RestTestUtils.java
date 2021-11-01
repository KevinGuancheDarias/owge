package com.kevinguanchedarias.owgejava;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class RestTestUtils {
    public static MockMvcRequestSpecification restGiven() {
        return RestAssuredMockMvc
                .given()
                .contentType(ContentType.JSON.withCharset(StandardCharsets.UTF_8));
    }
}
