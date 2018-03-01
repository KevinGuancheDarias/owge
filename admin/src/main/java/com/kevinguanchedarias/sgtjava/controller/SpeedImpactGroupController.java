package com.kevinguanchedarias.sgtjava.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.sgtjava.business.SpeedImpactGroupBo;
import com.kevinguanchedarias.sgtjava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.sgtjava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class SpeedImpactGroupController extends SgtCommonController<SpeedImpactGroup> implements Serializable {
	private static final long serialVersionUID = 6386317794880084887L;

	@Autowired
	private SpeedImpactGroupBo speedImpactGroupBo;

	private List<SpeedImpactGroup> objectList;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
		loadData();
	}

	public void newObject() {
		setSelectedObject(new SpeedImpactGroup());
	}

	public void save() {
		speedImpactGroupBo.save(getSelectedObject());
		closeDialog();
		loadData();
	}

	public void delete(SpeedImpactGroup selection) {
		speedImpactGroupBo.delete(selection);
		loadData();
	}

	public void closeDialog() {
		setSelectedObject(null);
	}

	public List<SpeedImpactGroup> getObjectList() {
		return objectList;
	}

	public void setObjectList(List<SpeedImpactGroup> objectList) {
		this.objectList = objectList;
	}

	@Override
	protected void loadData() {
		objectList = speedImpactGroupBo.findAll();
	}
}
