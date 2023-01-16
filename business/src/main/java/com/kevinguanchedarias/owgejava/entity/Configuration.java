package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCache;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithTaggableCacheListener;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "configuration")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(EntityWithTaggableCacheListener.class)
public class Configuration implements Serializable, EntityWithTaggableCache<String> {
    public static final String CONFIGURATION_CACHE_KEY = "configuration";

    @Serial
    private static final long serialVersionUID = -298326125776225265L;

    @Id
    private String name;

    @Column(name = "display_name", length = 400)
    private String displayName;

    private String value;

    @Builder.Default
    private Boolean privileged = false;

    public Configuration(String name, String value) {
        this.name = name;
        this.value = value;
        this.privileged = false;
    }

    public Configuration(String name, String value, String displayName) {
        this.name = name;
        this.value = value;
        this.displayName = displayName;
        this.privileged = false;
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public void setId(String id) {
        throw new ProgrammingException("Read only prop");
    }

    @Override
    public String getCacheTag() {
        return CONFIGURATION_CACHE_KEY;
    }
}
