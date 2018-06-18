package com.kevinguanchedarias.sgtjava.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.sgtjava.business.FactionBo;
import com.kevinguanchedarias.sgtjava.entity.Faction;
import com.kevinguanchedarias.sgtjava.entity.Improvement;
import com.kevinguanchedarias.sgtjava.util.ControllerUtil;

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

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		initFileUploadForImage();
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
		primaryResourceImage = getImageUploadHandler().handleFileUpload();
	}

	public void uploadSecondaryResourceImage() {
		secondaryResourceImage = getImageUploadHandler().handleFileUpload();
	}

	public void uploadEnergyImage() {
		energyImage = getImageUploadHandler().handleFileUpload();
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