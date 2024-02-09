/**
 *
 */
package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serial;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "admin_users")
public class AdminUser implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = 7181807205260800042L;

    @Id
    private Integer id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "can_add_admins", nullable = false)
    private Boolean canAddAdmins = false;
}
