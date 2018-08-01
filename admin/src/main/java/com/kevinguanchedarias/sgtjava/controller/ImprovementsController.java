package com.kevinguanchedarias.sgtjava.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.RandomUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.sgtjava.business.ImprovementBo;
import com.kevinguanchedarias.sgtjava.business.UnitTypeBo;
import com.kevinguanchedarias.sgtjava.entity.EntityWithImprovements;
import com.kevinguanchedarias.sgtjava.entity.ImprovementUnitType;
import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class ImprovementsController extends SgtCommonController<EntityWithImprovements> implements Serializable {
	private static final long serialVersionUID = -7787902572638441069L;

	private static final Logger LOGGER = Logger.getLogger(UpgradesController.class);

	private List<ImprovementUnitType> selectedObjectImprovementsUnitType;
	private ImprovementUnitType oldValuesForThis = new ImprovementUnitType();
	private List<UnitType> unitTypes;
	private ImprovementUnitType newImprovementUnitType;

	@Autowired
	private ImprovementBo improvementsBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
	}

	public void prepareForUpdate() {
		if (selectedObjectImprovementsUnitType != null) {
			selectedObjectImprovementsUnitType = null;
		}
	}

	/**
	 * 
	 * @param reset
	 *            should recreate the object
	 * @author Kevin Guanche Darias
	 */
	public void prepareForAddUnitTypeImprovement(Boolean reset) {
		loadUnitTypes();
		if (reset) {
			newImprovementUnitType = new ImprovementUnitType();
		} else {
			BeanUtils.copyProperties(newImprovementUnitType, oldValuesForThis);
		}
	}

	public void deleteUnitTypeImprovement(ImprovementUnitType selected) {
		for (Iterator<ImprovementUnitType> it = selectedObjectImprovementsUnitType.iterator(); it.hasNext();) {
			ImprovementUnitType current = it.next();
			if (current == selected) {
				improvementsBo.removeImprovementUnitType(current);
				it.remove();
				return;
			}
		}
	}

	public void saveUnitTypeImprovement() {
		prepareForAddUnitTypeImprovements();
		List<ImprovementUnitType> improvements = getSelectedObject().getImprovement().getUnitTypesUpgrades();
		try {
			checkDuplicate(improvements);
			checkValid();
			newImprovementUnitType.setUpgradeId(getSelectedObject().getImprovement());
			if (newImprovementUnitType.getId() == null) {
				newImprovementUnitType.setId(RandomUtils.nextInt());
				improvements.add(newImprovementUnitType);
			}
			newImprovementUnitType = null;
			execute("PF('newUnitImprovementsDialog').hide()");
		} catch (SgtBackendInvalidInputException e) {
			BeanUtils.copyProperties(oldValuesForThis, newImprovementUnitType);
			addErrorMessage("Error de entrada", e.getMessage());
			LOGGER.log(Level.INFO, e);
		}

	}

	public void loadSelectedObjectProxies() {
		if (selectedObjectImprovementsUnitType == null) {
			improvementsBo.loadImprovementUnitTypes(getSelectedObject().getImprovement());
			selectedObjectImprovementsUnitType = getSelectedObject().getImprovement().getUnitTypesUpgrades();
		}
	}

	public boolean hasAmountLimit() {
		return newImprovementUnitType != null && newImprovementUnitType.getUnitType() != null
				&& newImprovementUnitType.getUnitType().getMaxCount() != null
				&& newImprovementUnitType.getUnitType().getMaxCount() >= 0;
	}

	public List<ImprovementUnitType> getSelectedObjectImprovementsUnitType() {
		return selectedObjectImprovementsUnitType;
	}

	public void setSelectedObjectImprovementsUnitType(List<ImprovementUnitType> selectedObjectImprovementsUnitType) {
		this.selectedObjectImprovementsUnitType = selectedObjectImprovementsUnitType;
	}

	public ImprovementUnitType getOldValuesForThis() {
		return oldValuesForThis;
	}

	public void setOldValuesForThis(ImprovementUnitType oldValuesForThis) {
		this.oldValuesForThis = oldValuesForThis;
	}

	public List<UnitType> getUnitTypes() {
		return unitTypes;
	}

	public void setUnitTypes(List<UnitType> unitTypes) {
		this.unitTypes = unitTypes;
	}

	public ImprovementUnitType getNewImprovementUnitType() {
		return newImprovementUnitType;
	}

	public void setNewImprovementUnitType(ImprovementUnitType newImprovementUnitType) {
		this.newImprovementUnitType = newImprovementUnitType;
	}

	private void loadUnitTypes() {
		if (unitTypes == null) {
			unitTypes = unitTypeBo.findAll();
		}
	}

	/**
	 * Unused mandatory method
	 */
	@Override
	protected void loadData() {
		throw new UnsupportedOperationException(LOAD_DATA_NOT_AVAILABLE);
	}

	/**
	 * Will ensure UnitTypesImprovements is not null, and if it is, will set it
	 * 
	 * @author Kevin Guanche Darias
	 */
	private void prepareForAddUnitTypeImprovements() {
		if (getSelectedObject().getImprovement().getUnitTypesUpgrades() == null) {
			getSelectedObject().getImprovement().setUnitTypesUpgrades(new ArrayList<>());
		}
	}

	/**
	 * Will check if input is valid
	 * 
	 * @throws SgtBackendInvalidInputException
	 */
	private void checkValid() {
		if (!improvementsBo.isValid(newImprovementUnitType)) {
			throw new SgtBackendInvalidInputException("No se han rellenado todos los datos necesarios");
		}
	}

	/**
	 * Will check if there is a duplicated, and if there is will throw exception
	 * 
	 * @param improvements
	 * @throws com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException
	 */
	private void checkDuplicate(List<ImprovementUnitType> improvements) {
		if (improvementsBo.isDuplicated(improvements, newImprovementUnitType)) {
			throw new SgtBackendInvalidInputException("Ya se mejora eso, quizá quieras cambiar el valor del existente");
		}
	}
}
