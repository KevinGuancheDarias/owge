package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.dto.RequirementGroupDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithRequirementGroups;
import com.kevinguanchedarias.owgejava.entity.ObjectRelationToObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

import static com.kevinguanchedarias.owgejava.entity.RequirementGroup.REQUIREMENT_GROUP_CACHE_TAG;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
@AllArgsConstructor
public class RequirementGroupBo implements BaseBo<Integer, RequirementGroup, RequirementGroupDto> {
    @Serial
    private static final long serialVersionUID = 7208249910149543205L;

    private final transient RequirementGroupRepository repository;
    private final ObjectRelationBo objectRelationBo;
    private final RequirementBo requirementBo;
    private final transient ObjectRelationToObjectRelationRepository objectRelationRequirementGroupRepository;

    @Override
    public Class<RequirementGroupDto> getDtoClass() {
        return RequirementGroupDto.class;
    }

    @Override
    public JpaRepository<RequirementGroup, Integer> getRepository() {
        return repository;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional
    public RequirementGroup add(ObjectEnum targetObject, Integer referenceId,
                                RequirementGroupDto requirementGroupDto) {
        var currentGroup = new ObjectRelationToObjectRelation();
        var requirementGroup = new RequirementGroup();
        requirementGroup.setName(requirementGroupDto.getName());
        var saved = repository.save(requirementGroup);
        currentGroup.setMaster(objectRelationBo.findObjectRelationOrCreate(targetObject, referenceId));

        if (requirementGroupDto.getRequirements() != null) {
            requirementGroupDto.getRequirements().forEach(requirementInformationDto -> {
                var objectRelationDto = new ObjectRelationDto();
                objectRelationDto.dtoFromEntity(saved.getRelation());
                requirementInformationDto.setRelation(objectRelationDto);
                requirementBo.addRequirementFromDto(requirementInformationDto);
            });
        }
        currentGroup.setSlave(repository.save(requirementGroup).getRelation());
        objectRelationRequirementGroupRepository.save(currentGroup);
        return requirementGroup;
    }

    @TaggableCacheable(tags = REQUIREMENT_GROUP_CACHE_TAG, keySuffix = "#entityWithGroupRequirements.id")
    public List<RequirementGroup> findRequirements(EntityWithRequirementGroups entityWithGroupRequirements) {
        return doFindRequirements(entityWithGroupRequirements.getRelation().getId());
    }

    private List<RequirementGroup> doFindRequirements(Integer key) {
        return objectRelationRequirementGroupRepository.findByMasterId(key).stream()
                .map(relationToRelation -> (RequirementGroup) objectRelationBo
                        .unboxObjectRelation(relationToRelation.getSlave()))
                .toList();
    }
}
