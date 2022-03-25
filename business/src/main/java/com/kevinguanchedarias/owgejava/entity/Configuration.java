package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "configuration")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Configuration implements Serializable {
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
    }

    public Configuration(String name, String value, String displayName) {
        this.name = name;
        this.value = value;
        this.displayName = displayName;
    }
}
