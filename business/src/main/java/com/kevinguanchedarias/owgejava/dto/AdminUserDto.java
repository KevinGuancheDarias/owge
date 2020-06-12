package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.AdminUser;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class AdminUserDto implements WithDtoFromEntityTrait<AdminUser> {
	private Integer id;
	private String username;
	private Boolean enabled;
	private Boolean canAddAdmins;

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the username
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the enabled
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getEnabled() {
		return enabled;
	}

	/**
	 * @param enabled the enabled to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return the canAddAdmins
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getCanAddAdmins() {
		return canAddAdmins;
	}

	/**
	 * @param canAddAdmins the canAddAdmins to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanAddAdmins(Boolean canAddAdmins) {
		this.canAddAdmins = canAddAdmins;
	}

}
