package com.kevinguanchedarias.owgejava.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.jsf.controller.FileUploadController;
import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class FactionsController extends SgtCommonController<Faction> implements Serializable {
	private static final long serialVersionUID = -8672647492628259304L;

	private String primaryResourceImage;
	private String secondaryResourceImage;
	private String energyImage;

	private List<Faction> objectList;

	@Autowired
	private FactionBo factionBo;

	@ManagedProperty(value = "#{improvementsController}")
	private ImprovementsController improvementsController;

	private transient FileUploadController prImageUploadController;

	private transient FileUploadController srImageUploadController;

	private transient FileUploadController energyImageUploadController;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		initFileUploadForImage();
		prImageUploadController = createImageUploadController();
		srImageUploadController = createImageUploadController();
		energyImageUploadController = createImageUploadController();
		loadData();
	}

	public void newObject() {
		Faction selectedObject = new Faction();
		selectedObject.setMaxPlanets(1);
		selectedObject.setImprovement(new Improvement());
		improvementsController.setSelectedObject(selectedObject);
		clearUploadedImages();
		setSelectedObject(selectedObject);
	}

	public void prepareForUpdate() {
		clearUploadedImages();
		setupImageFromString(getSelectedObject().getImage());
		setupResourceImagesFromString(getSelectedObject().getPrimaryResourceImage(),
				getSelectedObject().getSecondaryResourceImage(), getSelectedObject().getEnergyImage());
		prepareImprovementsForUpdate(improvementsController, getSelectedObject());
	}

	public void save() {
		updateSelectedObjectImageIfRequired();
		updateResourceImagesIfRequired();
		saveWithName(factionBo, getSelectedObject().getName());
	}

	public void delete(Faction faction) {
		factionBo.delete(faction);
		loadData();
	}

	public void uploadPrimaryResourceImage() {
		primaryResourceImage = prImageUploadController.handleFileUpload();
	}

	public void uploadSecondaryResourceImage() {
		if (!srImageUploadController.getUploadedFile().getName().isEmpty()) {
			secondaryResourceImage = srImageUploadController.handleFileUpload();
			srImageUploadController.setUploadedFile(null);
		}
	}

	public void uploadEnergyImage() {
		energyImage = energyImageUploadController.handleFileUpload();
	}

	public String findPrimaryResourceImage() {
		return findFullPathOrNull(primaryResourceImage);
	}

	public String findSecundaryResourceImage() {
		return findFullPathOrNull(secondaryResourceImage);
	}

	public String findEnergyImage() {
		return findFullPathOrNull(energyImage);
	}

	public String findImageComponentsIds() {
		StringBuilder builder = new StringBuilder();
		return builder.append("objectImage image").append(" prImage primary_resource_image")
				.append(" srImage secondary_resource_image").append(" energyImage energy_image").toString();

	}

	public List<Faction> getObjectList() {
		return objectList;
	}

	public void setObjectList(List<Faction> objectList) {
		this.objectList = objectList;
	}

	public ImprovementsController getImprovementsController() {
		return improvementsController;
	}

	public void setImprovementsController(ImprovementsController improvementsController) {
		this.improvementsController = improvementsController;
	}

	public FileUploadController getPrImageUploadController() {
		return prImageUploadController;
	}

	public void setPrImageUploadController(FileUploadController prImageUploadController) {
		this.prImageUploadController = prImageUploadController;
	}

	public FileUploadController getSrImageUploadController() {
		return srImageUploadController;
	}

	public void setSrImageUploadController(FileUploadController srImageUploadController) {
		this.srImageUploadController = srImageUploadController;
	}

	public FileUploadController getEnergyImageUploadController() {
		return energyImageUploadController;
	}

	public void setEnergyImageUploadController(FileUploadController energyImageUploadController) {
		this.energyImageUploadController = energyImageUploadController;
	}

	@Override
	protected void loadData() {
		objectList = factionBo.findAll();
	}

	private void clearUploadedImages() {
		clearImageFileName();
		primaryResourceImage = null;
		secondaryResourceImage = null;
		energyImage = null;
	}

	private void updateResourceImagesIfRequired() {
		if (primaryResourceImage != null) {
			getSelectedObject().setPrimaryResourceImage(primaryResourceImage);
		}

		if (secondaryResourceImage != null) {
			getSelectedObject().setSecondaryResourceImage(secondaryResourceImage);
		}

		if (energyImage != null) {
			getSelectedObject().setEnergyImage(energyImage);
		}
	}

	private void setupResourceImagesFromString(String primaryResourceImage, String secondaryResourceImage,
			String energyImage) {
		if (StringUtils.isNotBlank(primaryResourceImage)) {
			this.primaryResourceImage = primaryResourceImage;
		}

		if (StringUtils.isNotBlank(secondaryResourceImage)) {
			this.secondaryResourceImage = secondaryResourceImage;
		}

		if (StringUtils.isNoneBlank(energyImage)) {
			this.energyImage = energyImage;
		}
	}
}