/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.pojo.SupportedOperations;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class SupportedOperationsBuilder {
	private SupportedOperations supportedOperations = new SupportedOperations();

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static SupportedOperationsBuilder create() {
		return new SupportedOperationsBuilder();
	}

	private SupportedOperationsBuilder() {
		// Do not use constructor, as it's less readable use, create() method
	}

	/**
	 * Returns the built instance reference
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperations build() {
		return supportedOperations;
	}

	/**
	 * All operations are supported, typically for admin panel usage
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withFullPrivilege() {
		withReadAll().withReadHidden().withReadOwned().withCreate().withUpdateOwned().withUpdateAny().withDeleteOwned()
				.withDeleteAny();
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withReadAll() {
		supportedOperations.setReadAll(true);
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withReadById() {
		supportedOperations.setReadById(true);
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withReadOwned() {
		supportedOperations.setReadOwned(true);
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withReadHidden() {
		supportedOperations.setReadHidden(true);
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withCreate() {
		supportedOperations.setCreate(true);
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withUpdateOwned() {
		supportedOperations.setUpdateOwned(true);
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withUpdateAny() {
		supportedOperations.setUpdateAny(true);
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withDeleteOwned() {
		supportedOperations.setDeleteOwned(true);
		return this;
	}

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SupportedOperationsBuilder withDeleteAny() {
		supportedOperations.setDeleteAny(true);
		return this;
	}
}
