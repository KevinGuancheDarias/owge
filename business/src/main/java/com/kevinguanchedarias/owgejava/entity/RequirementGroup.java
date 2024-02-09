package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCache;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithTaggableCacheListener;
import com.kevinguanchedarias.owgejava.entity.listener.RequirementGroupListener;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.*;

import jakarta.persistence.*;

import java.io.Serial;
import java.util.List;

/**
 * Represents a group of conditions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@Table(name = "requirement_group")
@EntityListeners({
        RequirementGroupListener.class,
        EntityWithTaggableCacheListener.class
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequirementGroup extends EntityWithRelationImp implements EntityWithTaggableCache<Integer> {
    public static final String REQUIREMENT_GROUP_CACHE_TAG = "requirement_group";

    @Serial
    private static final long serialVersionUID = 6503065882978157947L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String name;

    @Transient
    private List<RequirementInformation> requirements;

    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.REQUIREMENT_GROUP;
    }

    @Override
    public String getCacheTag() {
        return REQUIREMENT_GROUP_CACHE_TAG;
    }
}
