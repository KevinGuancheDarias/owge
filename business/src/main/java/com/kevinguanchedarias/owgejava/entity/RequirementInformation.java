package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCache;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithTaggableCacheListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.io.Serial;

/**
 * Has the special field secondValue , which represents the whole reason to
 * match ObjectRelation with a Requirement
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Table(name = "requirements_information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(EntityWithTaggableCacheListener.class)
public class RequirementInformation implements EntityWithTaggableCache<Integer> {
    public static final String REQUIREMENT_INFORMATION_CACHE_TAG = "requirement_information";

    @Serial
    private static final long serialVersionUID = -4898440527789250186L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relation_id")
    private ObjectRelation relation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @Column(name = "second_value")
    private Long secondValue;

    @Column(name = "third_value")
    private Long thirdValue;

    @Override
    public String getCacheTag() {
        return REQUIREMENT_INFORMATION_CACHE_TAG;
    }
}
