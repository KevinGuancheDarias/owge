/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.UpgradeTypeDto;
import com.kevinguanchedarias.owgejava.entity.UpgradeType;
import com.kevinguanchedarias.owgejava.repository.UpgradeTypeRepository;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/upgrade_type")
@AllArgsConstructor
public class AdminUpgradeTypeRestService
        implements CrudRestServiceTrait<Integer, UpgradeType, UpgradeTypeRepository, UpgradeTypeDto> {

    private UpgradeTypeRepository upgradeTypeRepository;
    private AutowireCapableBeanFactory beanFactory;

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait#
     * getRestCrudConfigBuilder()
     */
    @Override
    public RestCrudConfigBuilder<Integer, UpgradeType, UpgradeTypeRepository, UpgradeTypeDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, UpgradeType, UpgradeTypeRepository, UpgradeTypeDto> builder = RestCrudConfigBuilder
                .create();
        return builder.withBeanFactory(beanFactory).withRepository(upgradeTypeRepository).withDtoClass(UpgradeTypeDto.class)
                .withEntityClass(UpgradeType.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

}
