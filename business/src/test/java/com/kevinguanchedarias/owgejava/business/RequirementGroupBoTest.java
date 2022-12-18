package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.EntityWithRequirementGroups;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObjectRelationToObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.*;
import static com.kevinguanchedarias.owgejava.mock.RequirementGroupMock.*;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirementInformationDto;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = RequirementGroupBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        RequirementGroupRepository.class,
        ObjectRelationBo.class,
        RequirementBo.class,
        ObjectRelationToObjectRelationRepository.class,
})
class RequirementGroupBoTest {
    private final RequirementGroupBo requirementGroupBo;
    private final ObjectRelationToObjectRelationRepository objectRelationToObjectRelationRepository;
    private final ObjectRelationBo objectRelationBo;
    private final RequirementGroupRepository repository;
    private final RequirementBo requirementBo;

    @Autowired
    public RequirementGroupBoTest(
            RequirementGroupBo requirementGroupBo,
            ObjectRelationToObjectRelationRepository objectRelationToObjectRelationRepository,
            ObjectRelationBo objectRelationBo,
            RequirementGroupRepository repository,
            RequirementBo requirementBo
    ) {
        this.requirementGroupBo = requirementGroupBo;
        this.objectRelationToObjectRelationRepository = objectRelationToObjectRelationRepository;
        this.objectRelationBo = objectRelationBo;
        this.repository = repository;
        this.requirementBo = requirementBo;
    }

    @Test
    void add_should_work_without_requirements_to_process() {
        var targetObject = ObjectEnum.UNIT;
        var masterOr = givenObjectRelation();
        var savedGeneratedOr = givenObjectRelation(190);
        var requirementGroupDto = givenRequirementGroupDto();
        defineRelationOnSave(savedGeneratedOr);
        given(objectRelationBo.findObjectRelationOrCreate(targetObject, UNIT_ID_1)).willReturn(masterOr);

        var retVal = requirementGroupBo.add(targetObject, UNIT_ID_1, requirementGroupDto);

        var groupSaveCaptor = ArgumentCaptor.forClass(RequirementGroup.class);
        var relationToRelationCaptor = ArgumentCaptor.forClass(ObjectRelationToObjectRelation.class);
        verify(repository, times(2)).save(groupSaveCaptor.capture());
        verify(objectRelationToObjectRelationRepository, times(1)).save(relationToRelationCaptor.capture());
        var savedGroup = groupSaveCaptor.getAllValues().get(0);
        var savedOrToOr = relationToRelationCaptor.getValue();
        verifyNoInteractions(requirementBo);
        assertThat(savedGroup.getName()).isEqualTo(REQUIREMENT_GROUP_NAME);
        assertThat(savedOrToOr.getMaster()).isEqualTo(masterOr);
        assertThat(savedOrToOr.getSlave()).isEqualTo(savedGeneratedOr);
        assertThat(retVal).isSameAs(savedGroup);
    }

    @Test
    void add_should_work_wit_requirements() {
        var targetObject = ObjectEnum.UNIT;
        var masterOr = givenObjectRelation();
        var savedGeneratedOr = givenObjectRelation(190);
        var requirementGroupDto = givenRequirementGroupDto();
        var requirementInformationDto = givenRequirementInformationDto(220);
        requirementInformationDto.setRelation(null);
        requirementGroupDto.setRequirements(List.of(requirementInformationDto));
        defineRelationOnSave(savedGeneratedOr);
        given(objectRelationBo.findObjectRelationOrCreate(targetObject, UNIT_ID_1)).willReturn(masterOr);

        requirementGroupBo.add(targetObject, UNIT_ID_1, requirementGroupDto);

        assertThat(requirementInformationDto.getRelation().getId()).isEqualTo(190);
        verify(requirementBo, times(1)).addRequirementFromDto(requirementInformationDto);

    }

    @Test
    void findRequirements_should_work() {
        var intermediateRelation = givenObjectRelationToObjectRelation();
        var entityWithRequirements = mock(EntityWithRequirementGroups.class);
        var requirementGroup = givenRequirementGroup();
        var or = givenObjectRelation();
        given(entityWithRequirements.getRelation()).willReturn(or);
        given(objectRelationToObjectRelationRepository.findByMasterId(OBJECT_RELATION_ID)).willReturn(List.of(intermediateRelation));
        given(objectRelationBo.unboxObjectRelation(intermediateRelation.getSlave())).willReturn(requirementGroup);

        assertThat(requirementGroupBo.findRequirements(entityWithRequirements))
                .isNotEmpty()
                .contains(requirementGroup);

    }

    private void defineRelationOnSave(ObjectRelation savedGeneratedOr) {
        given(repository.save(any())).will(invocation -> {
            var saved = invocation.getArgument(0, RequirementGroup.class);
            saved.setRelation(savedGeneratedOr);
            return saved;
        });
    }
}
