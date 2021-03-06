/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.BaseBo;
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

/**
 * Adds the requirements crud for the specified object type
 *
 * @param <N> Numeric key of the target entity
 * @param <E> Target entity
 * @param <S> Business object to handle the entity
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface CrudWithRequirementsRestServiceTrait<N extends Number, E extends EntityWithId<N>, S extends BaseBo<N, E, D>, D extends DtoFromEntity<E>>
		extends CrudRestServiceNoOpEventsTrait<D, E> {

	public RestCrudConfigBuilder<N, E, S, D> getRestCrudConfigBuilder();

	/**
	 * Returns the object the entity <i>E</i> represents in the {@link ObjectEntity}
	 * table
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectEnum getObject();

	/**
	 * Test for correct {@link CrudWithRequirementsRestServiceTrait#getObject()}
	 * implementation <br>
	 * <b>NOTICE:</b> Won't work properly if you don't use @Scope,
	 * note @ApplicationScope doesn't work properly
	 * 
	 * @param objectEntityBo
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostConstruct
	@Autowired
	public default void init() {
		Logger.getLogger(getClass()).debug("Initializing crud with requirements");
		getBeanFactory().getBean(ObjectEntityBo.class).existsByDescriptionOrDie(getObject());
	}

	/**
	 * Finds the requirement informations for the given object
	 * 
	 * @param id The object that has the requirements
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("{id}/requirements")
	public default List<RequirementInformationDto> findRequirements(@PathVariable N id) {
		getBo().existsOrDie(id);
		List<RequirementInformationDto> requirements = getBeanFactory().getBean(DtoUtilService.class)
				.convertEntireArray(RequirementInformationDto.class, getBeanFactory().getBean(RequirementBo.class)
						.findRequirements(getObject(), Integer.valueOf(id.toString())));
		requirements.forEach(current -> current.setRelation(null));
		return requirements;

	}

	/**
	 * Adds a new requirement information
	 * 
	 * @todo Expensive method, in the future, to avoid DoS, limit calls to this
	 *       method
	 * @param id
	 * @param requirementInformationDto
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("{id}/requirements")
	public default RequirementInformationDto addRequirement(@PathVariable N id,
			@RequestBody RequirementInformationDto requirementInformationDto) {
		getBo().existsOrDie(id);
		requirementInformationDto.setRelation(new ObjectRelationDto(getObject().name(), (Integer) id));
		return getBeanFactory().getBean(RequirementBo.class).addRequirementFromDto(requirementInformationDto);
	}

	@DeleteMapping("{id}/requirements/{requirementInformationId}")
	public default ResponseEntity<Void> deleteRequirement(@PathVariable N id,
			@PathVariable Integer requirementInformationId) {
		getBo().existsOrDie(id);
		getBeanFactory().getBean(RequirementInformationBo.class).delete(requirementInformationId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@Override
	public default boolean filterGetResult(D dto, E savedEntity) {
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

	private S getBo() {
		return getRestCrudConfigBuilder().build().getBoService();
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
