package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ImprovementUnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.AffectedItem;
import com.kevinguanchedarias.owgejava.repository.ImprovementUnitTypeRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import org.apache.commons.lang3.EnumUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

@Service
public class ImprovementUnitTypeBo implements Serializable {
    @Serial
    private static final long serialVersionUID = -3323254005323573001L;

    private static final Logger LOGGER = Logger.getLogger(ImprovementUnitTypeBo.class);

    private static final Double DEFAULT_STEP = 10D;

    @Autowired
    private ImprovementUnitTypeRepository improvementUnitTypeRepository;

    @Autowired
    private ConfigurationBo configurationBo;

    @Autowired
    private ImprovementBo improvementBo;

    @Autowired
    private UnitTypeBo unitTypeBo;

    @Autowired
    private transient ExceptionUtilService exceptionUtilService;

    /**
     * Adds the specified unit type improvement to the target improvement (<b>
     * doesn't save</b>)
     *
     * @param improvementId
     * @param improvementUnitType
     * @return
     * @throws SgtBackendInvalidInputException If is not valid, or it's duplicated
     *                                         (for given <i>improvement</i>)
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public Improvement add(Integer improvementId, ImprovementUnitType improvementUnitType) {
        return add(improvementBo.findByIdOrDie(improvementId), improvementUnitType);
    }

    /**
     * Adds the specified unit type improvement to the target improvement (<b>
     * doesn't save</b>)
     *
     * @param improvement
     * @param improvementUnitType
     * @throws SgtBackendInvalidInputException If is not valid, or it's duplicated
     *                                         (for given <i>improvement</i>)
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public Improvement add(Improvement improvement, ImprovementUnitType improvementUnitType) {
        checkIsValid(improvementUnitType);
        if (isDuplicated(improvement, improvementUnitType)) {
            throw exceptionUtilService
                    .createExceptionBuilder(SgtBackendInvalidInputException.class,
                            "I18N_ERR_UNIT_IMPROVEMENT_DUPLICATED")
                    .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
        }
        improvementUnitType.setImprovementId(improvement);
        improvement.getUnitTypesUpgrades().add(improvementUnitType);
        return improvement;
    }

    /**
     * Will remove invalid improvements from Source List, Notice negative values are
     * considered valid EXPENSIVE method!
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
     * Returns true if {@link ImprovementUnitType} of same type already exists in
     * the target improvement <br>
     * <b>NOTICE:</b> It reloads the unit types of the <i>improvement</i>
     *
     * @param improvement         Improvement that has the list of unit type
     *                            improvements (because the uniqueness depends of
     *                            each parent improvement
     * @param improvementUnitType New unit type to check if it's dupllicated
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public boolean isDuplicated(Improvement improvement, ImprovementUnitType improvementUnitType) {
        loadImprovementUnitTypes(improvement);
        return isDuplicated(improvement.getUnitTypesUpgrades(), improvementUnitType);
    }

    /**
     * Will check if improvement is duplicated, known because, it has the same
     * type,the same target unit_type, and the same value NOTICE: Will not check the
     * object instance itself
     *
     * @param source
     * @param compared Item to compare against the list
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
        return isValidType(input) && isValidUnitType(input) && isValidValue(input);
    }

    /**
     * Will load the list of improvement of unit types for selected improvement,
     * used to avoid LazyException
     *
     * @param improvement
     * @author Kevin Guanche Darias
     */
    public void loadImprovementUnitTypes(Improvement improvement) {
        improvement.setUnitTypesUpgrades(improvementUnitTypeRepository.findByImprovementIdId(improvement.getId()));
    }

    public void removeImprovementUnitType(Integer improvementUnitTypeId) {
        try {
            improvementUnitTypeRepository.deleteById(improvementUnitTypeId);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.log(Level.INFO, e);
        }
    }

    /**
     * Will delete an improvement unit type
     *
     * @param improvementUnitType
     * @author Kevin Guanche Darias
     */
    public void removeImprovementUnitType(ImprovementUnitType improvementUnitType) {
        removeImprovementUnitType(improvementUnitType.getId());
    }

    /**
     * @param value
     * @param inputPercentage
     * @return
     * @todo As of 0.8.0 we don't use this function to detect unitType maxCount, nor
     * maxEnergy, users may not be happy with new working, so keep here till
     * future tells
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
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
     * Returns the total sum of the value for the specified improvement type for the
     * given user
     *
     * @param user
     * @param type The expected type
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated No need to use this, GroupedImprovement from
     * {@link ImprovementBo#findUserImprovement(UserStorage)} already
     * has a method, as you can see in the current body of this
     * function, And in case of use, may be <b>potentially bugged</b>,
     * as doesn't take into account the unit type id
     */
    @Deprecated(since = "0.8.0")
    public Long sumUnitTypeImprovementByUserAndImprovementType(UserStorage user, ImprovementTypeEnum type) {
        return improvementBo.findUserImprovement(user).findUnitTypeImprovement(type);
    }

    /**
     * Test if input arguments share same type and same unitType
     *
     * @param improvementUnitTypeDto
     * @param improvementUnitTypeDto2
     * @return True if both have the same type and the same unitType id
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public boolean isSameTarget(ImprovementUnitTypeDto improvementUnitTypeDto,
                                ImprovementUnitTypeDto improvementUnitTypeDto2) {
        return improvementUnitTypeDto.getType().equals(improvementUnitTypeDto2.getType())
                && improvementUnitTypeDto.getUnitTypeId().equals(improvementUnitTypeDto2.getUnitTypeId());
    }

    /**
     * Throws {@link NotFoundException} if the existing improvement doesn't contain
     * a unit type improvement with the specified id
     *
     * @param improvementId
     * @param unitTypeImprovementId
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public void checkHasUnitTypeImprovementById(Integer improvementId, Integer unitTypeImprovementId) {
        if (improvementBo.findByIdOrDie(improvementId).getUnitTypesUpgrades().stream()
                .filter(current -> current.getId().equals(unitTypeImprovementId)).findFirst().orElse(null) == null) {
            throw exceptionUtilService
                    .createExceptionBuilder(NotFoundException.class,
                            "I18N_ERR_IMPROVEMENT_HAS_NO_SUCH_UNIT_IMPROVEMENT")
                    .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS)
                    .withAffectedItem(new AffectedItem(
                            SpringRepositoryUtil.findEntityClass(improvementUnitTypeRepository), unitTypeImprovementId))
                    .build();
        }
    }

    private boolean isValidType(ImprovementUnitType input) {
        return !(input.getType() == null || input.getType().isEmpty()
                || !EnumUtils.isValidEnum(ImprovementTypeEnum.class, input.getType()));
    }

    private void checkValidType(ImprovementUnitType input) {
        if (!isValidType(input)) {
            throw exceptionUtilService
                    .createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_INVALID_TYPE")
                    .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
        }
    }

    private boolean isValidUnitType(ImprovementUnitType input) {
        return !(input.getUnitType() == null || input.getUnitType().getId() == null
                || !unitTypeBo.exists(input.getUnitType().getId()));
    }

    private void checkValidUnitType(ImprovementUnitType input) {
        if (!isValidUnitType(input)) {
            throw exceptionUtilService
                    .createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_INVALID_UNIT_TYPE")
                    .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
        } else {
            if (ImprovementTypeEnum.valueOf(input.getType()) == ImprovementTypeEnum.AMOUNT) {
                UnitType unitType = unitTypeBo.findById(input.getUnitType().getId());
                if (!unitType.hasMaxCount()) {
                    throw this.exceptionUtilService
                            .createExceptionBuilder(SgtBackendInvalidInputException.class,
                                    "I18N_ERR_UNIT_TYPE_UNLIMITED_COUNT")
                            .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS)
                            .build();
                }
            }
        }
    }

    private boolean isValidValue(ImprovementUnitType input) {
        return !(input.getValue() == null || input.getValue() == 0);
    }

    private void checkValidValue(ImprovementUnitType input) {
        if (!isValidValue(input)) {
            throw exceptionUtilService
                    .createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_INVALID_VALUE")
                    .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
        }
    }

    private void removeInvalidType(List<ImprovementUnitType> source) {
        for (Iterator<ImprovementUnitType> it = source.iterator(); it.hasNext(); ) {
            ImprovementUnitType currentImprovement = it.next();
            if (!isValidType(currentImprovement)) {
                it.remove();
            }
        }
    }

    private void removeInvalidUnitType(List<ImprovementUnitType> source) {
        for (Iterator<ImprovementUnitType> it = source.iterator(); it.hasNext(); ) {
            ImprovementUnitType currentImprovement = it.next();
            if (!isValidUnitType(currentImprovement)) {
                it.remove();
            }
        }
    }

    private void removeInvalidValue(List<ImprovementUnitType> source) {
        for (Iterator<ImprovementUnitType> it = source.iterator(); it.hasNext(); ) {
            ImprovementUnitType currentImprovement = it.next();
            if (!isValidValue(currentImprovement)) {
                it.remove();
            }
        }
    }

    private void checkIsValid(ImprovementUnitType improvementUnitType) {
        checkValidType(improvementUnitType);
        checkValidUnitType(improvementUnitType);
        checkValidValue(improvementUnitType);
    }
}
