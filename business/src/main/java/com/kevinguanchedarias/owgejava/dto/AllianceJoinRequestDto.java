/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AllianceJoinRequestDto implements DtoFromEntity<AllianceJoinRequest> {
    @EqualsAndHashCode.Include
    private Integer id;
    
    private UserStorageDto user;
    private AllianceDto alliance;

    @Override
    public void dtoFromEntity(AllianceJoinRequest joinRequest) {
        id = joinRequest.getId();
        user = new UserStorageDto();
        user.dtoFromEntity(joinRequest.getUser());
        alliance = new AllianceDto();
        alliance.dtoFromEntity(joinRequest.getAlliance());
    }
}
