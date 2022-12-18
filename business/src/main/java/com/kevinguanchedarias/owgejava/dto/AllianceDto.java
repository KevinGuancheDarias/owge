/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import org.hibernate.Hibernate;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
public class AllianceDto extends CommonDto<Integer, Alliance> {
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private UserStorageDto owner;

    @Override
    public void dtoFromEntity(Alliance alliance) {
        super.dtoFromEntity(alliance);
        if (Hibernate.isInitialized(alliance.getOwner())) {
            owner = new UserStorageDto();
            owner.dtoFromEntity(alliance.getOwner());
        }
    }

    /**
     * @return the owner
     * @since 0.7.0
     */
    public UserStorageDto getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     * @since 0.7.0
     */
    public void setOwner(UserStorageDto owner) {
        this.owner = owner;
    }

}
