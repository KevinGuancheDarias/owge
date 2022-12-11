package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Requirement;
import lombok.*;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDto implements DtoFromEntity<Requirement> {
    @EqualsAndHashCode.Include
    private Integer id;
    
    private String code;
    private String description;

    @Override
    public void dtoFromEntity(Requirement entity) {
        id = entity.getId();
        code = entity.getCode();
        description = entity.getDescription();
    }
}
