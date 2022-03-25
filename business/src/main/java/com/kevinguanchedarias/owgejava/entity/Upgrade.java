package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.UpgradeListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serial;
import java.util.List;

/**
 * All more* are percentages excluding moreMissions
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Table(name = "upgrades")
@EntityListeners(UpgradeListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Upgrade extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
    @Serial
    private static final long serialVersionUID = 905268542123876248L;

    @Builder.Default
    private Integer points = 0;

    @Builder.Default
    private Long time = 60L;

    @Builder.Default
    private Integer primaryResource = 100;

    @Builder.Default
    private Integer secondaryResource = 100;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type")
    private UpgradeType type;

    @Builder.Default
    private Float levelEffect = 20f;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "improvement_id")
    private Improvement improvement;

    @Builder.Default
    private Boolean clonedImprovements = false;

    @Transient
    private List<RequirementInformation> requirements;


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Upgrade other = (Upgrade) obj;
        if (getId() == null) {
            return other.getId() == null;
        } else return getId().equals(other.getId());
    }
}
