package com.kevinguanchedarias.sgtjava.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.sgtjava.business.UpgradeBo;
import com.kevinguanchedarias.sgtjava.business.UpgradeTypeBo;
import com.kevinguanchedarias.sgtjava.entity.Improvement;
import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.entity.Upgrade;
import com.kevinguanchedarias.sgtjava.entity.UpgradeType;
import com.kevinguanchedarias.sgtjava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class UpgradesController extends SgtCommonController<Upgrade> implements Serializable {

	private static final long serialVersionUID = 8887091674004395140L;

	private List<Upgrade> objectList;
	private List<UpgradeType> upgradeTypes;
	private List<UnitType> unitTypes;

	@Autowired
	private UpgradeTypeBo upgradeTypeBo;

	@Autowired
	private UpgradeBo upgradeBo;

	@ManagedProperty(value = "#{improvementsController}")
	private ImprovementsController improvementsController;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		initFileUploadForImage();
		loadData();
	}

	public void newObject() {
		Upgrade selectedObject = new Upgrade();
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
		saveWithName(upgradeBo, getSelectedObject().getName());
	}

	public void delete(Upgrade upgrade) {
		upgradeBo.delete(upgrade);
		loadData();
	}

	public List<Upgrade> getObjectList() {
		return objectList;
	}

	public void setObjectList(List<Upgrade> objectList) {
		this.objectList = objectList;
	}

	public List<UpgradeType> getUpgradeTypes() {
		return upgradeTypes;
	}

	public void setUpgradeTypes(List<UpgradeType> upgradeTypes) {
		this.upgradeTypes = upgradeTypes;
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
		objectList = upgradeBo.findAll();
		upgradeTypes = upgradeTypeBo.findAll();
	}

}
