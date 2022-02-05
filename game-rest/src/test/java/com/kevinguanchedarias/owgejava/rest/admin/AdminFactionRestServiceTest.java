package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.OwgeTestConfiguration;
import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.business.FactionSpawnLocationBo;
import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.dto.FactionSpawnLocationDto;
import com.kevinguanchedarias.owgejava.entity.FactionUnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.ImageStore;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.kevinguanchedarias.owgejava.RestTestUtils.restGiven;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.FACTION_ID;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.FACTION_UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.UNIT_TYPE_MAX_COUNT;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenEntity;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenOverride;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenSpawnLocationDto;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenUnitType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = AdminFactionRestService.class)
@Import(OwgeTestConfiguration.class)
@MockBean({
        FactionBo.class,
        ImageStoreBo.class,
        AutowireCapableBeanFactory.class,
        FactionSpawnLocationBo.class
})
class AdminFactionRestServiceTest {
    private static final String BASE_PATH = "admin/faction";

    private final FactionBo factionBo;
    private final ImageStoreBo imageStoreBo;
    private final AdminFactionRestService adminFactionRestService;
    private final FactionSpawnLocationBo factionSpawnLocationBo;

    @Autowired
    public AdminFactionRestServiceTest(
            FactionBo factionBo,
            ImageStoreBo imageStoreBo,
            AdminFactionRestService adminFactionRestService,
            WebApplicationContext webApplicationContext,
            FactionSpawnLocationBo factionSpawnLocationBo
    ) {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        this.factionBo = factionBo;
        this.imageStoreBo = imageStoreBo;
        this.adminFactionRestService = adminFactionRestService;
        this.factionSpawnLocationBo = factionSpawnLocationBo;
    }

    @Test
    void findUnitTypesOverrides_should_work() {
        var faction = givenEntity();
        faction.setUnitTypes(List.of(givenUnitType()));
        given(factionBo.findByIdOrDie(FACTION_ID)).willReturn(faction);

        var response = restGiven()
                .when()
                .get(BASE_PATH + "/{factionId}/unitTypes", FACTION_ID)

                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        var retVal = response.getBody().jsonPath().getList("", FactionUnitTypeDto.class);
        assertThat(retVal).hasSize(1);
        var returned = retVal.get(0);
        assertEquals(FACTION_UNIT_TYPE_ID, returned.getId());
        assertEquals(UNIT_TYPE_ID, returned.getUnitTypeId());
        assertNull(returned.getFactionId());
        assertEquals(UNIT_TYPE_MAX_COUNT, returned.getMaxCount());
    }

    @Test
    void findSpawnLocations_should_not_follow_when_faction_not_exists() {
        doThrow(new NotFoundException()).when(factionBo).existsOrDie(FACTION_ID);

        restGiven()
                .when()
                .get(BASE_PATH + "/{factionId}/spawn-locations", FACTION_ID)
                .then()
                .log().ifValidationFails()
                .statusCode(NOT_FOUND.value())
                .extract()
                .response();

        verify(factionBo, times(1)).existsOrDie(FACTION_ID);
        verify(factionSpawnLocationBo, never()).findByFaction(anyInt());
    }

    @Test
    void findSpawnLocations_should_work() {
        var dto = givenSpawnLocationDto();
        when(factionSpawnLocationBo.findByFaction(FACTION_ID)).thenReturn(List.of(dto));

        var response = restGiven()
                .when()
                .get(BASE_PATH + "/{factionId}/spawn-locations", FACTION_ID)
                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        verify(factionBo, times(1)).existsOrDie(FACTION_ID);
        verify(factionSpawnLocationBo, times(1)).findByFaction(FACTION_ID);
        var retVal = response.getBody().jsonPath().getList("", FactionSpawnLocationDto.class);
        assertThat(retVal).hasSize(1);
        var returned = retVal.get(0);
        assertEquals(dto, returned);
    }

    @Test
    void saveUnitTypes_should_save_new_overrides() {
        var overrides = List.of(givenOverride());

        restGiven()
                .body(overrides)
                .when()
                .put(BASE_PATH + "/{factionId}/unitTypes", FACTION_ID)
                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        verify(this.factionBo, times(1)).saveOverrides(FACTION_ID, overrides);
    }

    @Test
    void saveSpawnLocations_should_save_new_overrides() {
        var spawnLocations = List.of(givenSpawnLocationDto());

        restGiven()
                .body(spawnLocations)
                .when()
                .put(BASE_PATH + "/{factionId}/spawn-locations", FACTION_ID)
                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .extract()
                .response();

        verify(this.factionSpawnLocationBo, times(1)).saveSpawnLocations(FACTION_ID, spawnLocations);
    }

    @Test
    void beforeSave_should_load_images_when_ids_are_passed() {
        long image = 20;
        long prImage = 1;
        long srImage = 2;
        long energyImage = 3;
        var imageObject = ImageStore.builder().id(image).build();
        var prImageObject = ImageStore.builder().id(prImage).build();
        var srImageObject = ImageStore.builder().id(srImage).build();
        var energyImageObject = ImageStore.builder().id(energyImage).build();
        var factionDto = FactionDto.builder()
                .primaryResourceImage(prImage)
                .secondaryResourceImage(srImage)
                .energyImage(energyImage)
                .build();
        factionDto.setImage(image);
        var entity = givenEntity();
        when(imageStoreBo.findByIdOrDie(image)).thenReturn(imageObject);
        when(imageStoreBo.findByIdOrDie(prImage)).thenReturn(prImageObject);
        when(imageStoreBo.findByIdOrDie(srImage)).thenReturn(srImageObject);
        when(imageStoreBo.findByIdOrDie(energyImage)).thenReturn(energyImageObject);

        var result = adminFactionRestService.beforeSave(factionDto, entity);

        assertThat(result).isPresent();
        var parsedEntity = result.get();
        assertEquals(imageObject, parsedEntity.getImage());
        assertEquals(prImageObject, parsedEntity.getPrimaryResourceImage());
        assertEquals(srImageObject, parsedEntity.getSecondaryResourceImage());
        assertEquals(energyImageObject, parsedEntity.getEnergyImage());

    }
}
