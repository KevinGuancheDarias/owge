package com.kevinguanchedarias.sgtjava.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import org.springframework.orm.jpa.JpaSystemException;

import com.kevinguanchedarias.kevinsuite.commons.collection.MimeTypeCollection;
import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;
import com.kevinguanchedarias.kevinsuite.commons.jsf.controller.CommonController;
import com.kevinguanchedarias.kevinsuite.commons.jsf.controller.FileUploadController;
import com.kevinguanchedarias.kevinsuite.commons.pojo.MimeType;
import com.kevinguanchedarias.sgtjava.business.WithNameBo;
import com.kevinguanchedarias.sgtjava.entity.EntityWithImage;
import com.kevinguanchedarias.sgtjava.entity.EntityWithImprovements;
import com.kevinguanchedarias.sgtjava.entity.Improvement;

public abstract class SgtCommonController<E extends SimpleIdEntity> extends CommonController {
	private static final Logger LOGGER = Logger.getLogger(SgtCommonController.class);

	public static final String HIDE_WRITE_DIALOG = "PF('writeDialog').hide()";
	public static final String DUPLICATED_TITLE = "Ya existe un elemento con ese nombre";
	public static final String INTERNAL_ERROR_DETAIL = "Error desconocido de base de datos";
	public static final String LOAD_DATA_NOT_AVAILABLE = "No se puede invocar loadData() en este controller";
	public static final String URL_TO_IMAGES = "/images/";

	private E selectedObject;
	private FileUploadController imageUploadHandler;
	private String imageFileName = null;

	/**
	 * Will execute client side code
	 * 
	 * @param command
	 *            Javascript line to execute, for example PF('dialog').hide()
	 * @author Kevin Guanche Darias
	 */
	public void execute(String command) {
		RequestContext.getCurrentInstance().execute(command);
	}

	/**
	 * Will handle the upload of an image <br />
	 * This method is usually used inside h:inputFile
	 * 
	 * @author Kevin Guanche Darias
	 */
	public void uploadImage() {
		imageFileName = imageUploadHandler.handleFileUpload();
	}

	/**
	 * Will return URL to image
	 * 
	 * @return full path to image or null
	 * @author Kevin Guanche Darias
	 */
	public String findImageWithUrlPath() {
		return findFullPathOrNull(imageFileName);
	}

	/**
	 * Will remove uploadController image, useful to ensure old image gets
	 * removed
	 * 
	 * @author Kevin Guanche Darias
	 */
	public void clearImageFileName() {
		imageFileName = null;
	}

	/**
	 * Will set the image to this one
	 * 
	 * @param name
	 *            of the image file (Nullsafe!)
	 * @author Kevin Guanche Darias
	 */
	public void setupImageFromString(String image) {
		if (StringUtils.isNotBlank(image)) {
			imageFileName = image;
		}
	}

	public E getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(E selectedObject) {
		this.selectedObject = selectedObject;
	}

	public FileUploadController getImageUploadHandler() {
		return imageUploadHandler;
	}

	public void setImageUploadHandler(FileUploadController imageUploadHandler) {
		this.imageUploadHandler = imageUploadHandler;
	}

	protected abstract void loadData();

	/**
	 * Will check if selectedObject.improvement is null, and if it is, will
	 * instantiate a new one Will add the entity to the ImprovementsController
	 * instance
	 * 
	 * @author Kevin Guanche Darias
	 */
	protected void prepareImprovementsForUpdate(ImprovementsController improvementsController,
			EntityWithImprovements selectedObject) {
		improvementsController.setSelectedObject(selectedObject);
		improvementsController.prepareForUpdate();

		if (selectedObject.getImprovement() == null) {
			selectedObject.setImprovement(new Improvement());
		}
	}

	/**
	 * Will handle the save exceptions, so in case of duplicate entry, will show
	 * a nice message to the user
	 * 
	 * @param sourceBo
	 * @param name
	 *            The name of the object (used to print it to the user if it's
	 *            duplicated)
	 * @param ex
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings("rawtypes")
	protected void handleSaveException(WithNameBo sourceBo, String name, JpaSystemException ex) {
		if (sourceBo.findOneByName(name) != null) {
			addErrorMessage(DUPLICATED_TITLE, "El nombre " + name + " ya está siendo utilizado");
			LOGGER.log(Level.DEBUG, ex);
		} else {
			addErrorMessage(INTERNAL_ERROR_TITLE, INTERNAL_ERROR_DETAIL);
			LOGGER.log(Level.ERROR, ex);
		}
	}

	/**
	 * Will save to database, handling db exceptions and known SGT exceptions
	 * 
	 * @param withNameBo
	 *            - Bo that extends WithNameBo, used to call the save method
	 * @author Kevin Guanche Darias
	 */
	protected void saveWithName(WithNameBo<E> withNameBo, String name) {
		try {
			withNameBo.saveAndFlush(selectedObject);
			loadData();
			selectedObject = null;
		} catch (JpaSystemException e) {
			handleSaveException(withNameBo, name, e);
		}
	}

	/**
	 * Will initialize the FileUploadController<br />
	 * Should be run in the PostConstruct method of controller wanting to upload
	 * image
	 * 
	 * @author Kevin Guanche Darias
	 */
	protected void initFileUploadForImage() {
		imageUploadHandler = new FileUploadController();
		imageUploadHandler.setValidMimes(new MimeTypeCollection(MimeType.newCollectionPngJpgGif()));
		imageUploadHandler
				.setPropertiesFile(getClass().getClassLoader().getResourceAsStream("fileuploadcontroller.properties"));
	}

	/**
	 * Will update the selectedObject image ONLY if new image has been uploaded
	 * 
	 * @author Kevin Guanche Darias
	 */
	protected void updateSelectedObjectImageIfRequired() {
		if (imageFileName != null) {
			((EntityWithImage) selectedObject).setImage(imageFileName);
		}
	}

	protected String findFullPathOrNull(String imageFileName) {
		if (imageFileName != null) {
			return findUrlWithContextPath() + "" + imageFileName;
		} else {
			return null;
		}
	}

	private String findUrlWithContextPath() {
		return "/" + URL_TO_IMAGES;
	}

}
