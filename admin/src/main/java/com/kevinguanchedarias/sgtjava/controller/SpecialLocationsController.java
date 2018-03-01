package com.kevinguanchedarias.sgtjava.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.sgtjava.business.GalaxyBo;
import com.kevinguanchedarias.sgtjava.business.PlanetBo;
import com.kevinguanchedarias.sgtjava.business.SpecialLocationBo;
import com.kevinguanchedarias.sgtjava.entity.Galaxy;
import com.kevinguanchedarias.sgtjava.entity.Improvement;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.SpecialLocation;
import com.kevinguanchedarias.sgtjava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class SpecialLocationsController extends SgtCommonController<SpecialLocation> implements Serializable {
	private static final long serialVersionUID = 9169467092946623034L;

	private List<SpecialLocation> objectList;
	private List<Galaxy> availableGalaxies;

	@Autowired
	private SpecialLocationBo specialLocationBo;

	@Autowired
	private GalaxyBo galaxyBo;

	@Autowired
	private PlanetBo planetBo;

	@ManagedProperty(value = "#{improvementsController}")
	private ImprovementsController improvementsController;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		initFileUploadForImage();
		availableGalaxies = galaxyBo.findAll();
		loadData();
	}

	public void newObject() {
		SpecialLocation selectedObject = new SpecialLocation();
		selectedObject.setImprovement(new Improvement());
		improvementsController.setSelectedObject(selectedObject);
		clearImageFileName();
		setSelectedObject(selectedObject);
	}

	public void prepareForUpdate() {
		clearImageFileName();
		if (getSelectedObject().getAssignedPlanet() != null) {
			getSelectedObject().setAssignedPlanet(planetBo.refresh(getSelectedObject().getAssignedPlanet()));
		}
		setupImageFromString(getSelectedObject().getImage());

	}

	public void delete(SpecialLocation specialLocation) {
		specialLocationBo.delete(specialLocation);
		loadData();
	}

	public void save() {
		updateSelectedObjectImageIfRequired();
		saveWithName(specialLocationBo, getSelectedObject().getName());
	}

	public void assignPlanet() {
		Planet planet = specialLocationBo.assignPlanet(getSelectedObject());
		getSelectedObject().setAssignedPlanet(planet);
	}

	public void deleteAssignedPlanet() {
		getSelectedObject().setAssignedPlanet(null);
	}

	public List<SpecialLocation> getObjectList() {
		return objectList;
	}

	public void setObjectList(List<SpecialLocation> objectList) {
		this.objectList = objectList;
	}

	@Override
	protected void loadData() {
		objectList = specialLocationBo.findAll();
	}

	public ImprovementsController getImprovementsController() {
		return improvementsController;
	}

	public void setImprovementsController(ImprovementsController improvementsController) {
		this.improvementsController = improvementsController;
	}

	public List<Galaxy> getAvailableGalaxies() {
		return availableGalaxies;
	}

	public void setAvailableGalaxies(List<Galaxy> availableGalaxies) {
		this.availableGalaxies = availableGalaxies;
	}

}
