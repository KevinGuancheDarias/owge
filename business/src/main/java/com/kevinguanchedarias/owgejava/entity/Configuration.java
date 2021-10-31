package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "configuration")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Configuration implements Serializable {
    private static final long serialVersionUID = -298326125776225265L;

    @Id
    private String name;

    @Column(name = "display_name", length = 400, nullable = true)
    private String displayName;

    private String value;

    private Boolean privileged = false;

    public Configuration(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Configuration(String name, String value, String displayName) {
        this.name = name;
        this.value = value;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
     * @deprecated Use isPrivileged() instead, avoid null problems, and uses the
     * "isPrivileged"
     */
    @Deprecated(since = "0.9.0")
    public Boolean getPrivileged() {
        return privileged;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
     * @since 0.9.0
     */
    public boolean isPrivileged() {
        return Boolean.TRUE.equals(privileged);
    }

    public void setPrivileged(Boolean privileged) {
        this.privileged = privileged;
    }
}
