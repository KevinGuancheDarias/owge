/**
 *
 */
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

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
