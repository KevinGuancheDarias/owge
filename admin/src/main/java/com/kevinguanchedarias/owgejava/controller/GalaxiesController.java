package com.kevinguanchedarias.owgejava.controller;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class GalaxiesController extends SgtCommonController<Galaxy> implements Serializable {
	private static final long serialVersionUID = 1230267869729181548L;

	private static final Logger LOGGER = Logger.getLogger(GalaxiesController.class);

	private List<Galaxy> objectList;

	@Autowired
	private GalaxyBo galaxyBo;

	@PostConstruct
	private void init() {
		ControllerUtil.enableAutowire(this);
		loadData();
	}

	public void newObject() {
		setSelectedObject(new Galaxy());
	}

	public void delete(Galaxy galaxy) {
		try {
			galaxyBo.delete(galaxy);
			loadData();
		} catch (SgtBackendInvalidInputException e) {
			addErrorMessage("Galaxia no vacía",
					"No se puede borrar una galaxia en las que hay planetas que ya son propiedad de un jugador");
			LOGGER.info(e);
		}
	}

	public void save() throws InterruptedException {
		if (galaxyBo.findOneByName(getSelectedObject().getName()) != null) {
			addErrorMessage(DUPLICATED_TITLE,
					"El nombre " + getSelectedObject().getName() + " ya está siendo utilizado");
			return;
		}
		try {
			galaxyBo.canSave(getSelectedObject());
		} catch (SgtBackendInvalidInputException e) {
			addErrorMessage("Error al guardar", e.getMessage());
			LOGGER.info(e);
			return;
		}

		addMessage("Creando galaxia", "Se te avisará cuando la galaxia esté creada");

		Future<Galaxy> future = galaxyBo.saveAsync(getSelectedObject());
		while (!future.isDone()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.fatal(e);
				addErrorMessage("Error al guardar la galaxia", "No se pudo dormir al hilo de la petición");
				throw e;
			}
		}
		loadData();
		addMessage("Acción completada con éxito", "Se creo la galaxia con éxito");
	}

	public List<Galaxy> getObjectList() {
		return objectList;
	}

	public void setObjectList(List<Galaxy> objectList) {
		this.objectList = objectList;
	}

	@Override
	protected void loadData() {
		objectList = galaxyBo.findAll();
	}
}
