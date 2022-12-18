package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCache;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithTaggableCacheListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(EntityWithTaggableCacheListener.class)
public class Rule implements EntityWithTaggableCache<Integer> {
    public static final String RULE_CACHE_TAG = "rules";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    String type;
    String originType;
    Long originId;
    String destinationType;
    Long destinationId;
    String extraArgs;

    @Override
    public String getCacheTag() {
        return RULE_CACHE_TAG;
    }
}
