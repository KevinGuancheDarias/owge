package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.AdminUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AdminUserDto implements DtoFromEntity<AdminUser> {
    @EqualsAndHashCode.Include
    private Integer id;

    private String username;
    private Boolean enabled;
    private Boolean canAddAdmins;

    @Override
    public void dtoFromEntity(AdminUser entity) {
        id = entity.getId();
        username = entity.getUsername();
        enabled = entity.getEnabled();
        canAddAdmins = entity.getCanAddAdmins();
    }
}
