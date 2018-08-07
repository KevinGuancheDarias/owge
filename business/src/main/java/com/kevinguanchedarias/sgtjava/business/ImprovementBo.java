package com.kevinguanchedarias.sgtjava.business;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.entity.Improvement;
import com.kevinguanchedarias.sgtjava.entity.ImprovementUnitType;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.ImprovementType;
import com.kevinguanchedarias.sgtjava.repository.ImprovementRepository;
import com.kevinguanchedarias.sgtjava.repository.UnitTypeRepository;

@Service
public class ImprovementBo implements Serializable {
	private static final long serialVersionUID = -3323254005323573001L;

	private static final Logger LOGGER = Logger.getLogger(ImprovementBo.class);

	private static final Double DEFAULT_STEP = 10D;
	@Autowired
	private UnitTypeRepository unitTypeRepository;

	@Autowired
	private ImprovementRepository improvementRepository;

	@Autowired
	private ObtainedUpradeBo obtainedUpgradeBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private ConfigurationBo configurationBo;

	/**
	 * Will remove invalid improvements from Source List, Notice negative values
	 * are considered valid EXPENSIVE method!
	 * 
	 * @param source
	 * @return List without invalid improvements
	 * @author Kevin Guanche Darias
	 */
	public List<ImprovementUnitType> removeInvalidFromList(List<ImprovementUnitType> source) {
		List<ImprovementUnitType> result = source;

		removeInvalidType(result);
		removeInvalidUnitType(result);
		removeInvalidValue(result);

		return result;
	}

	/**
	 * Will check if improvement is duplicated, known because, it has the same
	 * type,the same target unit_type, and the same value NOTICE: Will not check
	 * the object instance itself
	 * 
	 * @param source
	 * @param compared
	 *            Item to compare against the list
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public Boolean isDuplicated(List<ImprovementUnitType> source, ImprovementUnitType compared) {
		for (ImprovementUnitType currentImprovement : source) {
			if (currentImprovement != compared && currentImprovement.getType().equals(compared.getType())
					&& currentImprovement.getUnitType().equals(compared.getUnitType())) {

				return true;
			}
		}
		return false;
	}

	/**
	 * Will check if input is valid. WARNING: EXPENSIVE method!
	 * 
	 * @param input
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public Boolean isValid(ImprovementUnitType input) {
		return validType(input) && validUnitType(input) && validValue(input);
	}

	/**
	 * Will load the list of improvement of unit types for selected improvement,
	 * used to avoid LazyException
	 * 
	 * @param improvement
	 * @author Kevin Guanche Darias
	 */
	public void loadImprovementUnitTypes(Improvement improvement) {
		improvement.setUnitTypesUpgrades(improvementRepository.findByImprovementIdId(improvement.getId()));
	}

	/**
	 * Will delete an improvement by id
	 * 
	 * @param improvement
	 * @author Kevin Guanche Darias
	 */
	public void removeImprovementUnitType(Integer improvement) {
		try {
			improvementRepository.delete(improvement);
		} catch (EmptyResultDataAccessException e) {
			LOGGER.log(Level.INFO, e);
		}
	}

	/**
	 * Will delete an improvement
	 * 
	 * @param improvement
	 * @author Kevin Guanche Darias
	 */
	public void removeImprovementUnitType(ImprovementUnitType improvement) {
		removeImprovementUnitType(improvement.getId());
	}

	public Double computeImprovementValue(double value, double inputPercentage) {
		double retVal = value;
		double step = Double
				.parseDouble(configurationBo.findOrSetDefault("IMPROVEMENT_STEP", DEFAULT_STEP.toString()).getValue());
		double pendingPercentage = inputPercentage;
		while (pendingPercentage > 0) {
			retVal += retVal * (step / 100);
			pendingPercentage -= step;
		}
		return retVal;
	}

	/**
	 * Returns the total sum of the value for the specified improvement type for
	 * the given user
	 * 
	 * @param user
	 * @param type
	 *            The expected type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long sumUnitTypeImprovementByUserAndImprovementType(UserStorage user, ImprovementType type) {
		return obtainedUnitBo.sumUnitTypeImprovementByUserAndImprovementType(user, type)
				+ obtainedUpgradeBo.sumUnitTypeImprovementByUserAndImprovementType(user, type);
	}

	private boolean validType(ImprovementUnitType input) {
		return !(input.getType() == null || input.getType().isEmpty()
				|| !EnumUtils.isValidEnum(ImprovementType.class, input.getType()));
	}

	private boolean validUnitType(ImprovementUnitType input) {
		return !(input.getUnitType() == null || input.getUnitType().getId() == null
				|| !unitTypeRepository.exists(input.getUnitType().getId()));
	}

	private boolean validValue(ImprovementUnitType input) {
		return !(input.getValue() == null || input.getValue() == 0);
	}

	private void removeInvalidType(List<ImprovementUnitType> source) {
		for (Iterator<ImprovementUnitType> it = source.iterator(); it.hasNext();) {
			ImprovementUnitType currentImprovement = it.next();
			if (!validType(currentImprovement)) {
				it.remove();
			}
		}
	}

	private void removeInvalidUnitType(List<ImprovementUnitType> source) {
		for (Iterator<ImprovementUnitType> it = source.iterator(); it.hasNext();) {
			ImprovementUnitType currentImprovement = it.next();
			if (!validUnitType(currentImprovement)) {
				it.remove();
			}
		}
	}

	private void removeInvalidValue(List<ImprovementUnitType> source) {
		for (Iterator<ImprovementUnitType> it = source.iterator(); it.hasNext();) {
			ImprovementUnitType currentImprovement = it.next();
			if (!validValue(currentImprovement)) {
				it.remove();
			}
		}
	}
}
