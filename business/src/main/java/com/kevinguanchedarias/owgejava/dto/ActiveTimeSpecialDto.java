/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ActiveTimeSpecialDto implements DtoFromEntity<ActiveTimeSpecial> {
    @EqualsAndHashCode.Include
    private Long id;

    private Integer timeSpecial;
    private TimeSpecialStateEnum state;
    private Date activationDate;
    private Date expiringDate;
    private Long pendingTime;
    private Date readyDate;

    @Override
    public void dtoFromEntity(ActiveTimeSpecial entity) {
        id = entity.getId();
        state = entity.getState();
        activationDate = entity.getActivationDate();
        expiringDate = entity.getExpiringDate();
        pendingTime = entity.getPendingTime();
        readyDate = entity.getReadyDate();
        timeSpecial = entity.getTimeSpecial().getId();
    }
}
