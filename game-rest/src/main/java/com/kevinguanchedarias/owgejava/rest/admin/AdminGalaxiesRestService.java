package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.GalaxyDto;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

/**
 * @author Kevin Guanche Darias
 * @since 0.9.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/galaxy")
@AllArgsConstructor
public class AdminGalaxiesRestService implements CrudRestServiceTrait<Integer, Galaxy, GalaxyRepository, GalaxyDto> {
    private final GalaxyRepository galaxyRepository;
    private final AutowireCapableBeanFactory beanFactory;

    @Override
    public RestCrudConfigBuilder<Integer, Galaxy, GalaxyRepository, GalaxyDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, Galaxy, GalaxyRepository, GalaxyDto> builder = RestCrudConfigBuilder.create();
        return builder.withBeanFactory(beanFactory).withRepository(galaxyRepository).withEntityClass(Galaxy.class)
                .withDtoClass(GalaxyDto.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

    @GetMapping("/{id}/has-players")
    public boolean hasPlayers(@PathVariable Integer id) {
        return !galaxyRepository.hasPlayers(id, PageRequest.of(0, 1)).isEmpty();
    }

}
