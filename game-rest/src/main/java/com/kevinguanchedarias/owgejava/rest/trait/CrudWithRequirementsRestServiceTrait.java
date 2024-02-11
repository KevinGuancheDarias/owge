package com.kevinguanchedarias.owgejava.rest.trait;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.ObjectEntityBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.RequirementInformationBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Adds the requirements crud for the specified object type
 *
 * @param <N> Numeric key of the target entity
 * @param <E> Target entity
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public interface CrudWithRequirementsRestServiceTrait<
        N extends Number,
        E extends EntityWithId<N>,
        R extends JpaRepository<E, N>,
        D extends DtoFromEntity<E>
        > extends CrudRestServiceNoOpEventsTrait<D, E> {

    RestCrudConfigBuilder<N, E, R, D> getRestCrudConfigBuilder();

    /**
     * Returns the object the entity <i>E</i> represents in the {@link ObjectEntity}
     * table
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    ObjectEnum getObject();

    /**
     * Test for correct {@link CrudWithRequirementsRestServiceTrait#getObject()}
     * implementation <br>
     * <b>NOTICE:</b> Won't work properly if you don't use @Scope,
     * note @ApplicationScope doesn't work properly
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @PostConstruct
    @Autowired
    default void init() {
        getBeanFactory().getBean(ObjectEntityBo.class).existsByDescriptionOrDie(getObject());
    }

    /**
     * Finds the requirement informations for the given object
     *
     * @param id The object that has the requirements
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @GetMapping("{id}/requirements")
    default List<RequirementInformationDto> findRequirements(@PathVariable N id) {
        SpringRepositoryUtil.existsOrDie(getRepository(), id);
        List<RequirementInformationDto> requirements = getBeanFactory().getBean(DtoUtilService.class)
                .convertEntireArray(RequirementInformationDto.class, getBeanFactory().getBean(RequirementInformationBo.class)
                        .findRequirements(getObject(), Integer.valueOf(id.toString())));
        requirements.forEach(current -> current.setRelation(null));
        return requirements;

    }

    /**
     * Adds a new requirement information
     *
     * @todo Expensive method, in the future, to avoid DoS, limit calls to this
     * method
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @PostMapping("{id}/requirements")
    default RequirementInformationDto addRequirement(@PathVariable N id,
                                                     @RequestBody RequirementInformationDto requirementInformationDto) {
        SpringRepositoryUtil.existsOrDie(getRepository(), id);
        requirementInformationDto.setRelation(new ObjectRelationDto(getObject().name(), (Integer) id));
        return getBeanFactory().getBean(RequirementBo.class).addRequirementFromDto(requirementInformationDto);
    }

    @DeleteMapping("{id}/requirements/{requirementInformationId}")
    default ResponseEntity<Void> deleteRequirement(@PathVariable N id,
                                                   @PathVariable Integer requirementInformationId) {
        SpringRepositoryUtil.existsOrDie(getRepository(), id);
        getBeanFactory().getBean(RequirementInformationBo.class).delete(requirementInformationId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    default boolean filterGetResult(D dto, E savedEntity) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String filterByRequirementName = request.getParameter("filterByRequirementName");
        String filterByRequirementSecondValue = request.getParameter("filterByRequirementSecondValue");
        String filterByRequirementThirdValue = request.getParameter("filterByRequirementThirdValue");
        if (!StringUtils.isEmpty(filterByRequirementName) && !StringUtils.isEmpty(filterByRequirementSecondValue)) {
            Map<String, Map<String, String>> filters = converCsvToMap(filterByRequirementName,
                    filterByRequirementSecondValue, filterByRequirementThirdValue);
            List<RequirementInformationDto> requirements = findRequirements(savedEntity.getId());
            return requirements.stream().allMatch(current -> {
                Map<String, String> values = filters.get(current.getRequirement().getCode());
                return values == null || (current.getSecondValue().equals(Long.valueOf(values.get("secondValue")))
                        && (StringUtils.isEmpty(values.get("thirdValue"))
                        || current.getThirdValue().equals(Long.valueOf(values.get("thirdValue")))));
            });
        } else {
            return true;
        }
    }

    private R getRepository() {
        return getRestCrudConfigBuilder().build().getRepository();
    }

    private BeanFactory getBeanFactory() {
        return getRestCrudConfigBuilder().build().getBeanFactory();
    }

    private Map<String, Map<String, String>> converCsvToMap(String names, String secondValues, String thirdValues) {
        String[] namesSplit = names.split(",");
        String[] secondValuesSplit = secondValues.split(",");
        String[] thirdValuesSplit = thirdValues.split(",");
        Map<String, Map<String, String>> retVal = new HashMap<>();
        IntStream.range(0, namesSplit.length).forEach(index -> {
            Map<String, String> valuesMap = new HashMap<>();
            valuesMap.put("secondValue", secondValuesSplit[index]);
            valuesMap.put("thirdValue", thirdValuesSplit[index]);
            retVal.put(namesSplit[index], valuesMap);
        });
        return retVal;
    }
}
