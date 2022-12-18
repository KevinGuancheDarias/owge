/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO for {@link Configuration}
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.4
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConfigurationDto implements DtoFromEntity<Configuration> {
    @EqualsAndHashCode.Include
    private String name;

    private String displayName;
    private String value;

    @Override
    public void dtoFromEntity(Configuration entity) {
        name = entity.getName();
        displayName = entity.getDisplayName();
        value = entity.getValue();
    }
}
