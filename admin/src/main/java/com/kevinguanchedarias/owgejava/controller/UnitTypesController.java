package com.kevinguanchedarias.owgejava.controller;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import com.kevinguanchedarias.owgejava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class UnitTypesController extends SgtCommonController<UnitType> {

	private static final Logger LOGGER = Logger.getLogger(UpgradesController.class);

	@Autowired
	private UnitTypeBo unitTypeBo;

	private List<UnitType> unitTypes;
	private boolean isUnlimited = true;
	private MissionSupportEnum[] missionSupportEnumValues = MissionSupportEnum.values();

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		initFileUploadForImage();
		loadData();
		newObject();
	}

	@Override
	public void setSelectedObject(UnitType type) {
		isUnlimited = type.getMaxCount() == null;
		super.setSelectedObject(type);
	}

	public void newObject() {
		setSelectedObject(new UnitType());
	}

	public void save() {
		updateSelectedObjectImageIfRequired();
		saveWithName(unitTypeBo, getSelectedObject().getName());
	}

	public void delete(UnitType unitType) {
		try {
			unitTypeBo.delete(unitType);
		} catch (RuntimeException e) {
			LOGGER.log(Level.INFO, e);
			addErrorMessage(INTERNAL_ERROR_TITLE, INTERNAL_ERROR_DETAIL);
		}
		loadData();
	}

	public String displayMaxCount(UnitType type) {
		return type.getMaxCount() == null ? "unlimited" : type.getMaxCount().toString();
	}

	public void prepareForUpdate() {
		clearImageFileName();
		setupImageFromString(getSelectedObject().getImage());
	}

	public List<UnitType> getUnitTypes() {
		return unitTypes;
	}

	public void setUnitTypes(List<UnitType> unitTypes) {
		this.unitTypes = unitTypes;
	}

	public boolean isUnlimited() {
		return isUnlimited;
	}

	public void setUnlimited(boolean isUnlimited) {
		this.isUnlimited = isUnlimited;
	}

	public MissionSupportEnum[] getMissionSupportEnumValues() {
		return missionSupportEnumValues;
	}

	public void setMissionSupportEnumValues(MissionSupportEnum[] missionSupportEnumValues) {
		this.missionSupportEnumValues = missionSupportEnumValues;
	}

	/**
	 * Will load unit types in the property (used to avoid JSF multiple getter
	 * call madness
	 * 
	 * @author Kevin Guanche Darias
	 */
	@Override
	protected void loadData() {
		unitTypes = unitTypeBo.findAll();
	}
}