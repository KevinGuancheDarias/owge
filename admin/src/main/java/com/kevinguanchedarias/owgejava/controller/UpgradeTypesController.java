package com.kevinguanchedarias.owgejava.controller;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.owgejava.business.UpgradeTypeBo;
import com.kevinguanchedarias.owgejava.entity.UpgradeType;
import com.kevinguanchedarias.owgejava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class UpgradeTypesController extends SgtCommonController<UpgradeType> {

	private static final Logger LOGGER = Logger.getLogger(UpgradesController.class);

	private List<UpgradeType> upgradeTypes;

	@Autowired
	private UpgradeTypeBo upgradeTypeBo;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		loadData();
	}

	public void newObject() {
		setSelectedObject(new UpgradeType());
	}

	public void save() {
		saveWithName(upgradeTypeBo, getSelectedObject().getName());
	}

	public void delete(UpgradeType upgradeType) {
		try {
			upgradeTypeBo.delete(upgradeType);
		} catch (RuntimeException e) {
			LOGGER.log(Level.INFO, e);
			addErrorMessage(INTERNAL_ERROR_TITLE, INTERNAL_ERROR_DETAIL);
		}
		loadData();
	}

	public List<UpgradeType> getUpgradeTypes() {
		return upgradeTypes;
	}

	public void setUpgradeTypes(List<UpgradeType> upgradeTypes) {
		this.upgradeTypes = upgradeTypes;
	}

	/**
	 * Will load upgrade types in the property (used to avoid JSF multiple
	 * getter call madness
	 * 
	 * @author Kevin Guanche Darias
	 */
	@Override
	protected void loadData() {
		upgradeTypes = upgradeTypeBo.findAll();
	}
}