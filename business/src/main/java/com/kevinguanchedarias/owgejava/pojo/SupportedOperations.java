/**
 * 
 */
package com.kevinguanchedarias.owgejava.pojo;

/**
 * When creating crud controllers, One may define the supported operations by
 * the controller
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class SupportedOperations {
	private boolean readAll;
	private boolean readById;
	private boolean readOwned;
	private boolean readHidden;
	private boolean create;
	private boolean updateOwned;
	private boolean updateAny;
	private boolean deleteOwned;
	private boolean deleteAny;

	/**
	 * @since 0.8.0
	 * @return the read
	 */
	public boolean canReadAll() {
		return readAll;
	}

	/**
	 * @since 0.8.0
	 * @param read the read to set
	 */
	public void setReadAll(boolean read) {
		this.readAll = read;
	}

	/**
	 * @since 0.8.0
	 * @return the readById
	 */
	public boolean canReadById() {
		return readById;
	}

	/**
	 * @since 0.8.0
	 * @param readById the readById to set
	 */
	public void setReadById(boolean readById) {
		this.readById = readById;
	}

	/**
	 * Can read entities owned by the requester user
	 * 
	 * @since 0.8.0
	 * @return the readOwned
	 */
	public boolean canReadOwned() {
		return readOwned;
	}

	/**
	 * Can read entities owned by the requester user
	 * 
	 * @since 0.8.0
	 * @param readOwned the readOwned to set
	 */
	public void setReadOwned(boolean readOwned) {
		this.readOwned = readOwned;
	}

	/**
	 * Can read entities with disabled or hidden state (unless where the ownership
	 * takes place)
	 * 
	 * @since 0.8.0
	 * @return the readHidden
	 */
	public boolean canReadHidden() {
		return readHidden;
	}

	/**
	 * Can read entities with disabled or hidden state (unless where the ownership
	 * takes place)
	 * 
	 * @since 0.8.0
	 * @param readHidden the readHidden to set
	 */
	public void setReadHidden(boolean readHidden) {
		this.readHidden = readHidden;
	}

	/**
	 * @since 0.8.0
	 * @return the create
	 */
	public boolean canCreate() {
		return create;
	}

	/**
	 * @since 0.8.0
	 * @param create the create to set
	 */
	public void setCreate(boolean create) {
		this.create = create;
	}

	/**
	 * Can update <b>only</b> entities owned by the requester user
	 * 
	 * @since 0.8.0
	 * @return the updateOwned
	 */
	public boolean canUpdateOwned() {
		return updateOwned;
	}

	/**
	 * Can update <b>only</b> entities owned by the requester user
	 * 
	 * @since 0.8.0
	 * @param updateOwned the updateOwned to set
	 */
	public void setUpdateOwned(boolean updateOwned) {
		this.updateOwned = updateOwned;
	}

	/**
	 * Can update <b>any</b> entity
	 * 
	 * @since 0.8.0
	 * @return the updateAny
	 */
	public boolean canUpdateAny() {
		return updateAny;
	}

	/**
	 * Can update <b>any</b> entity
	 * 
	 * @since 0.8.0
	 * @param updateAny the updateAny to set
	 */
	public void setUpdateAny(boolean updateAny) {
		this.updateAny = updateAny;
	}

	/**
	 * Can only delete items that are owned by the requester user
	 * 
	 * @since 0.8.0
	 * @return the deleteOwned
	 */
	public boolean canDeleteOwned() {
		return deleteOwned;
	}

	/**
	 * Can only delete items that are owned by the requester user
	 * 
	 * @since 0.8.0
	 * @param deleteOwned the deleteOwned to set
	 */
	public void setDeleteOwned(boolean deleteOwned) {
		this.deleteOwned = deleteOwned;
	}

	/**
	 * Can delete any item
	 * 
	 * @since 0.8.0
	 * @return the deleteAny
	 */
	public boolean canDeleteAny() {
		return deleteAny;
	}

	/**
	 * Can delete any item
	 * 
	 * @since 0.8.0
	 * @param deleteAny the deleteAny to set
	 */
	public void setDeleteAny(boolean deleteAny) {
		this.deleteAny = deleteAny;
	}

}
