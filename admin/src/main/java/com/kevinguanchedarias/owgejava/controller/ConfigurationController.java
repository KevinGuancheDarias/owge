package com.kevinguanchedarias.owgejava.controller;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.jsf.controller.CommonController;
import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class ConfigurationController extends CommonController {

	@Autowired
	private ConfigurationBo configurationBo;

	private List<Configuration> allConfigurations;

	private String oldValue;
	private Configuration selectedObject;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		loadData();
	}

	public void delete(Configuration configuration) {
		throw new NotImplementedException("Feature not implemented");
	}

	public void save() {
		try {
			configurationBo.save(selectedObject);
		} catch (SgtBackendInvalidInputException e) {
			addErrorMessage(INVALID_INPUT, e.getMessage());
			selectedObject.setValue(oldValue);
		}
	}

	public void rowSelect() {
		oldValue = selectedObject.getValue();
	}

	public List<Configuration> getAllConfigurations() {
		return allConfigurations;
	}

	public void setAllConfigurations(List<Configuration> allConfigurations) {
		this.allConfigurations = allConfigurations;
	}

	public Configuration getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(Configuration selectedObject) {
		this.selectedObject = selectedObject;
	}

	private void loadData() {
		allConfigurations = configurationBo.findAllNonPrivileged();
	}
}
