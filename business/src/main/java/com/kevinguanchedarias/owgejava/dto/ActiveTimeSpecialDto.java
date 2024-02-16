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
    private Long pendingMillis;
    private Date readyDate;

    @Override
    public void dtoFromEntity(ActiveTimeSpecial entity) {
        id = entity.getId();
        state = entity.getState();
        activationDate = entity.getActivationDate();
        expiringDate = entity.getExpiringDate();
        readyDate = entity.getReadyDate();
        calculatePendingMillis();
        timeSpecial = entity.getTimeSpecial().getId();
    }

    /**
     * Calculates the pending millis, is a public method because maybe it has to be recalculated
     * on cache results by @{@link com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable}
     */
    public void calculatePendingMillis() {
        if (state == TimeSpecialStateEnum.ACTIVE) {
            pendingMillis = (expiringDate.getTime() - new Date().getTime());
        } else {
            pendingMillis = (readyDate.getTime() - new Date().getTime());
        }
    }
}
