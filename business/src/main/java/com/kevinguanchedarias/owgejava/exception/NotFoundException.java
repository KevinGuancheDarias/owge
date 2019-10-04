package com.kevinguanchedarias.owgejava.exception;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.pojo.AffectedItem;
import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;

public class NotFoundException extends CommonException {
	private static final long serialVersionUID = -4957558025066063907L;
	private static final String GENERIC_I18N_STRING = "I18N_ERR_GENERIC_ITEM_NOT_FOUND";

	private final Class<?> affectedItemType;
	private final Number affectedItemId;

	public static NotFoundException fromAffected(Class<?> clazz, Number id) {
		return new NotFoundException(GENERIC_I18N_STRING, clazz, id);
	}

	/**
	 * Empty constructor with generic error message (I18N ready)
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public NotFoundException() {
		super(GENERIC_I18N_STRING);
		affectedItemType = null;
		affectedItemId = null;

	}

	public NotFoundException(String message) {
		super(message);
		affectedItemType = null;
		affectedItemId = null;
	}

	/**
	 * @param gameBackendErrorPojo
	 * @param cause
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public NotFoundException(GameBackendErrorPojo gameBackendErrorPojo, Exception cause) {
		super(gameBackendErrorPojo, cause);
		affectedItemType = null;
		affectedItemId = null;
	}

	protected NotFoundException(String message, Class<?> affectedItemType, Number affectedItemId) {
		super(message);
		this.affectedItemType = affectedItemType;
		this.affectedItemId = affectedItemId;
		getExtra().put("affectedItem", new AffectedItem(this));
	}

	/**
	 * @since 0.8.0
	 * @return the affectedItemType
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Class<? extends JpaRepository> getAffectedItemType() {
		return (Class<? extends JpaRepository>) affectedItemType;
	}

	/**
	 * @since 0.8.0
	 * @return the affectedItemId
	 */
	public Number getAffectedItemId() {
		return affectedItemId;
	}

}
