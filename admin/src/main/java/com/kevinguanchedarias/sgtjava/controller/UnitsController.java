package com.kevinguanchedarias.sgtjava.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.sgtjava.business.UnitBo;
import com.kevinguanchedarias.sgtjava.business.UnitTypeBo;
import com.kevinguanchedarias.sgtjava.entity.Improvement;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class UnitsController extends SgtCommonController<Unit> implements Serializable {
	private static final long serialVersionUID = 8778646365045778391L;

	private List<Unit> objectList;
	private List<UnitType> unitTypes;

	@Autowired
	private UnitBo unitBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	@ManagedProperty(value = "#{improvementsController}")
	private ImprovementsController improvementsController;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		initFileUploadForImage();
		loadData();
	}

	public void newObject() {
		Unit selectedObject = new Unit();
		selectedObject.setImprovement(new Improvement());
		improvementsController.setSelectedObject(selectedObject);
		clearImageFileName();
		setSelectedObject(selectedObject);
	}

	public void prepareForUpdate() {
		clearImageFileName();
		setupImageFromString(getSelectedObject().getImage());
		prepareImprovementsForUpdate(improvementsController, getSelectedObject());
	}

	public void save() {
		updateSelectedObjectImageIfRequired();
		saveWithName(unitBo, getSelectedObject().getName());
	}

	public void delete(Unit unit) {
		unitBo.delete(unit);
		loadData();
	}

	public List<Unit> getObjectList() {
		return objectList;
	}

	public void setObjectList(List<Unit> objectList) {
		this.objectList = objectList;
	}

	public List<UnitType> getUnitTypes() {
		return unitTypes;
	}

	public void setUnitTypes(List<UnitType> unitTypes) {
		this.unitTypes = unitTypes;
	}

	public ImprovementsController getImprovementsController() {
		return improvementsController;
	}

	public void setImprovementsController(ImprovementsController improvementsController) {
		this.improvementsController = improvementsController;
	}

	@Override
	protected void loadData() {
		objectList = unitBo.findAll();
		unitTypes = unitTypeBo.findAll();
	}
}
