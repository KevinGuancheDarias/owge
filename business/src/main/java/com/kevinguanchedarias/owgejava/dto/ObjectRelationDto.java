package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for {@link ObjectRelation}
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ObjectRelationDto implements WithDtoFromEntityTrait<ObjectRelation> {
    private Integer id;
    private String objectCode;
    private Integer referenceId;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public ObjectRelationDto(String objectCode, Integer referenceId) {
        super();
        this.objectCode = objectCode;
        this.referenceId = referenceId;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait#dtoFromEntity(
     * java.lang.Object)
     */
    @Override
    public void dtoFromEntity(ObjectRelation entity) {
        WithDtoFromEntityTrait.super.dtoFromEntity(entity);
        objectCode = entity.getObject().getCode();
    }

}
