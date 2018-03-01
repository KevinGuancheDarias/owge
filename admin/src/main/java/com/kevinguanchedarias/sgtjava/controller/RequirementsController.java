package com.kevinguanchedarias.sgtjava.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;
import com.kevinguanchedarias.sgtjava.business.FactionBo;
import com.kevinguanchedarias.sgtjava.business.GalaxyBo;
import com.kevinguanchedarias.sgtjava.business.ObjectEntityBo;
import com.kevinguanchedarias.sgtjava.business.RequirementBo;
import com.kevinguanchedarias.sgtjava.business.RequirementInformationBo;
import com.kevinguanchedarias.sgtjava.business.SpecialLocationBo;
import com.kevinguanchedarias.sgtjava.business.UpgradeBo;
import com.kevinguanchedarias.sgtjava.entity.ObjectEntity;
import com.kevinguanchedarias.sgtjava.entity.ObjectRelation;
import com.kevinguanchedarias.sgtjava.entity.Requirement;
import com.kevinguanchedarias.sgtjava.entity.RequirementInformation;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.util.ControllerUtil;

@ManagedBean
@ViewScoped
public class RequirementsController extends SgtCommonController<RequirementInformation> implements Serializable {
	private static final long serialVersionUID = 7265291163538823986L;

	private RequirementTargetObject requirementTargetObject;
	private Integer targetReference;
	private List<RequirementInformation> objectList;
	private List<Requirement> requirementTypes;
	private Requirement selectedRequirement;
	private String selectedRequirementLabel;
	private transient Object selectedRequirementValue;
	private String selectedRequirementSecondLabel;
	private Long selectedRequirementSecondValue;

	@SuppressWarnings("rawtypes")
	private transient List selectedRequirementAvailableData;

	@Autowired
	private RequirementBo requirementBo;

	@Autowired
	private SpecialLocationBo specialLocationBo;

	@Autowired
	private FactionBo factionBo;

	@Autowired
	private UpgradeBo upgradeBo;

	@Autowired
	private GalaxyBo galaxyBo;

	@Autowired
	private ObjectEntityBo objectEntityBo;

	@Autowired
	private RequirementInformationBo requirementInformationBo;

	@PostConstruct
	public void init() {
		ControllerUtil.enableAutowire(this);
	}

	public void loadRequirements(RequirementTargetObject type, Integer reference) {
		requirementTargetObject = type;
		targetReference = reference;

		loadData();
	}

	public void newObject() {
		setSelectedObject(new RequirementInformation());
		selectedRequirement = null;
		selectedRequirementLabel = null;
		selectedRequirementValue = null;
		selectedRequirementSecondLabel = null;
		selectedRequirementSecondValue = null;
		loadRequirementTypes();
	}

	public void selectRequirement() {
		selectedRequirementSecondLabel = null;
		switch (selectedRequirement.getCode()) {
		case "HAVE_SPECIAL_LOCATION":
			loadSpecialLocations();
			break;
		case "BEEN_RACE":
			loadFactions();
			break;
		case "UPGRADE_LEVEL":
			loadUpgrades();
			break;
		case "WORST_PLAYER":
			selectedRequirementLabel = null;
			break;
		case "HOME_GALAXY":
			loadGalaxies();
			break;
		default:
			resetSelectedRequirement();
			addErrorMessage("No existe este tipo de requisito",
					"Es probable que se trate de un error de programación!");
			break;
		}
	}

	public void save() {
		requirementInformationBo.save(prepareRequirementInformation());
		loadData();
		execute("PF('addEditRequirementDialog').hide()");
	}

	public void delete(RequirementInformation requirementInformation) {
		requirementInformationBo.deleteRequirementInformation(requirementInformation);
		loadData();
	}

	public String getSecondValueDescription(RequirementInformation requirementInformation) {
		return requirementInformationBo.getSecondValueDescription(requirementInformation);
	}

	public RequirementTargetObject getRequirementTargetObject() {
		return requirementTargetObject;
	}

	public void setRequirementTargetObject(RequirementTargetObject requirementTargetObject) {
		this.requirementTargetObject = requirementTargetObject;
	}

	public Integer getTargetReference() {
		return targetReference;
	}

	/**
	 * Target object id, example: id of the upgrade, or id of the unit..
	 * 
	 * @param targetReference
	 * @author Kevin Guanche Darias
	 */
	public void setTargetReference(Integer targetReference) {
		this.targetReference = targetReference;
	}

	public List<RequirementInformation> getObjectList() {
		return objectList;
	}

	public void setObjectList(List<RequirementInformation> objectList) {
		this.objectList = objectList;
	}

	public List<Requirement> getRequirementTypes() {
		return requirementTypes;
	}

	public void setRequirementTypes(List<Requirement> requirementTypes) {
		this.requirementTypes = requirementTypes;
	}

	public Requirement getSelectedRequirement() {
		return selectedRequirement;
	}

	public void setSelectedRequirement(Requirement selectedRequirement) {
		this.selectedRequirement = selectedRequirement;
	}

	@SuppressWarnings("rawtypes")
	public List getSelectedRequirementAvailableData() {
		return selectedRequirementAvailableData;
	}

	@SuppressWarnings("rawtypes")
	public void setSelectedRequirementAvailableData(List selectedRequirementAvailableData) {
		this.selectedRequirementAvailableData = selectedRequirementAvailableData;
	}

	public String getSelectedRequirementLabel() {
		return selectedRequirementLabel;
	}

	public void setSelectedRequirementLabel(String selectedRequirementLabel) {
		this.selectedRequirementLabel = selectedRequirementLabel;
	}

	public Object getSelectedRequirementValue() {
		return selectedRequirementValue;
	}

	public void setSelectedRequirementValue(Object selectedRequirementValue) {
		this.selectedRequirementValue = selectedRequirementValue;
	}

	public String getSelectedRequirementSecondLabel() {
		return selectedRequirementSecondLabel;
	}

	public void setSelectedRequirementSecondLabel(String selectedRequirementSecondLabel) {
		this.selectedRequirementSecondLabel = selectedRequirementSecondLabel;
	}

	public Long getSelectedRequirementSecondValue() {
		return selectedRequirementSecondValue;
	}

	public void setSelectedRequirementSecondValue(Long selectedRequirementSecondValue) {
		this.selectedRequirementSecondValue = selectedRequirementSecondValue;
	}

	@Override
	protected void loadData() {
		objectList = requirementBo.getRequirements(requirementTargetObject, targetReference);
	}

	/**
	 * Will load the requirement types if not done before
	 * 
	 * @author Kevin Guanche Darias
	 */
	private void loadRequirementTypes() {
		if (requirementTypes == null) {
			requirementTypes = requirementBo.findAll();
		}
	}

	private void loadSpecialLocations() {
		selectedRequirementSecondLabel = null;
		selectedRequirementLabel = "Seleccionar ubicación:";
		selectedRequirementAvailableData = specialLocationBo.findAll();
	}

	private void loadFactions() {
		selectedRequirementSecondLabel = null;
		selectedRequirementLabel = "Seleccionar facción:";
		selectedRequirementAvailableData = factionBo.findAll();
	}

	private void loadUpgrades() {
		selectedRequirementLabel = "Seleccionar mejora:";
		selectedRequirementAvailableData = upgradeBo.findAll();
		selectedRequirementSecondLabel = "Nivel:";
	}

	private void loadGalaxies() {
		selectedRequirementSecondLabel = null;
		selectedRequirementLabel = "Seleccionar galaxia:";
		selectedRequirementAvailableData = galaxyBo.findAll();
	}

	private void resetSelectedRequirement() {
		selectedRequirement = null;
		selectedRequirementLabel = null;
		selectedRequirementAvailableData = null;
	}

	private ObjectEntity findObjectEntity(RequirementTargetObject target) {
		return objectEntityBo.getByDescription(target);
	}

	private RequirementInformation prepareRequirementInformation() {
		RequirementInformation retVal = new RequirementInformation();

		ObjectRelation relation = requirementInformationBo.findObjectRelation(requirementTargetObject, targetReference);
		if (relation == null) {
			relation = new ObjectRelation();
			relation.setObject(findObjectEntity(requirementTargetObject));
			relation.setReferenceId(targetReference);
		}

		retVal.setRelation(relation);
		retVal.setRequirement(selectedRequirement);
		if (selectedRequirementValue != null) {
			retVal.setSecondValue(((SimpleIdEntity) selectedRequirementValue).getId().longValue());
		}
		if (selectedRequirementSecondValue != null) {
			retVal.setThirdValue(selectedRequirementSecondValue);
		}

		return retVal;
	}
}
