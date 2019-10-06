/**
 * 
 */
package com.kevinguanchedarias.owgejava.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.AdminUser;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface AdminUserRepository extends JpaRepository<AdminUser, Integer> {

}
