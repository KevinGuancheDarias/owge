package com.kevinguanchedarias.owgejava.business.rule;

import com.kevinguanchedarias.owgejava.business.rule.itemtype.RuleItemTypeProvider;
import com.kevinguanchedarias.owgejava.business.rule.type.RuleTypeProvider;
import com.kevinguanchedarias.owgejava.business.unit.util.UnitTypeInheritanceFinderService;
import com.kevinguanchedarias.owgejava.converter.rule.RuleDtoToEntityConverter;
import com.kevinguanchedarias.owgejava.converter.rule.RuleEntityToDtoConverter;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokePredicateLambdaAnswer;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.kevinguanchedarias.owgejava.mock.RuleMock.FIRST_EXTRA_ARG;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.ORIGIN_ID;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.ORIGIN_TYPE;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.SECOND_EXTRA_ARG;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.TYPE;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRule;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleDto;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleItemTypeDescriptor;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleTypeDescriptorDto;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                RuleBo.class,
                RuleEntityToDtoConverter.class,
                RuleDtoToEntityConverter.class,
                DefaultConversionService.class
        }
)
@MockBean({
        RuleRepository.class,
        RuleItemTypeProvider.class,
        RuleTypeProvider.class,
        TaggableCacheManager.class,
        UnitTypeInheritanceFinderService.class
})
class RuleBoTest {
    private final RuleRepository ruleRepository;
    private final RuleBo ruleBo;
    private final RuleItemTypeProvider ruleItemTypeProvider;
    private final RuleTypeProvider ruleTypeProvider;
    private final TaggableCacheManager taggableCacheManager;
    private final UnitTypeInheritanceFinderService unitTypeInheritanceFinderService;

    @Autowired
    public RuleBoTest(
            RuleRepository ruleRepository,
            RuleBo ruleBo,
            DefaultConversionService conversionService,
            Collection<Converter<?, ?>> converters,
            RuleItemTypeProvider ruleItemTypeProvider,
            RuleTypeProvider ruleTypeProvider,
            TaggableCacheManager taggableCacheManager,
            UnitTypeInheritanceFinderService unitTypeInheritanceFinderService
    ) {
        this.ruleRepository = ruleRepository;
        this.ruleBo = ruleBo;
        this.ruleItemTypeProvider = ruleItemTypeProvider;
        this.ruleTypeProvider = ruleTypeProvider;
        this.taggableCacheManager = taggableCacheManager;
        this.unitTypeInheritanceFinderService = unitTypeInheritanceFinderService;
        converters.forEach(conversionService::addConverter);
    }

    @Test
    void findByOriginTypeAndOriginId_should_work() {
        var rule = givenRule();
        when(ruleRepository.findByOriginTypeAndOriginId(ORIGIN_TYPE, ORIGIN_ID)).thenReturn(List.of(rule));

        var result = ruleBo.findByOriginTypeAndOriginId(ORIGIN_TYPE, ORIGIN_ID);

        verify(ruleRepository, times(1)).findByOriginTypeAndOriginId(ORIGIN_TYPE, ORIGIN_ID);
        assertThat(result).hasSize(1);
    }

    @Test
    void findByType_should_return_types_by_id() {
        var rule = givenRule();
        when(ruleRepository.findByType(TYPE)).thenReturn(List.of(rule));

        var result = ruleBo.findByType(TYPE);

        verify(ruleRepository, times(1)).findByType(TYPE);
        assertThat(result).hasSize(1);
        var resultDto = result.get(0);
        assertEquals(rule.getId(), resultDto.getId());
        assertEquals(rule.getType(), resultDto.getType());
        assertEquals(rule.getOriginType(), resultDto.getOriginType());
        assertEquals(rule.getOriginId(), resultDto.getOriginId());
        assertEquals(rule.getDestinationType(), resultDto.getDestinationType());
        assertEquals(rule.getDestinationId(), resultDto.getDestinationId());
        assertEquals(List.of(FIRST_EXTRA_ARG, SECOND_EXTRA_ARG), resultDto.getExtraArgs());
    }

    @Test
    void deleteById_should_work() {
        int id = 4;

        ruleBo.deleteById(id);

        verify(ruleRepository, times(1)).deleteById(id);
    }

    @Test
    void save_should_work() {
        var dto = givenRuleDto();
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = ruleBo.save(dto);

        assertEquals(dto, saved);
        var captor = ArgumentCaptor.forClass(Rule.class);
        verify(ruleRepository, times(1)).save(captor.capture());
        var entityToBeSaved = captor.getValue();
        assertEquals(dto.getId(), entityToBeSaved.getId());
        assertEquals(dto.getType(), entityToBeSaved.getType());
        assertEquals(dto.getOriginType(), entityToBeSaved.getOriginType());
        assertEquals(dto.getOriginId(), entityToBeSaved.getOriginId());
        assertEquals(dto.getDestinationType(), entityToBeSaved.getDestinationType());
        assertEquals(dto.getDestinationId(), entityToBeSaved.getDestinationId());
        assertEquals(FIRST_EXTRA_ARG + "#" + SECOND_EXTRA_ARG, entityToBeSaved.getExtraArgs());
        verify(taggableCacheManager, times(1)).evictByCacheTag(RuleBo.RULE_CACHE_TAG, dto.getId());
    }

    @Test
    void save_should_not_clear_id_cache_if_entity_is_new() {
        var dto = givenRuleDto().toBuilder().id(0).build();
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ruleBo.save(dto);

        verify(taggableCacheManager, never()).evictByCacheTag(any(), any());
    }

    @Test
    void findItemTypeDescriptor_should_work() {
        var itemType = "UNIT";
        var expectedResult = givenRuleItemTypeDescriptor();
        when(ruleItemTypeProvider.getRuleItemTypeId()).thenReturn(itemType);
        when(ruleItemTypeProvider.findRuleItemTypeDescriptor()).thenReturn(expectedResult);

        var result = this.ruleBo.findItemTypeDescriptor(itemType);

        verify(ruleItemTypeProvider, times(1)).getRuleItemTypeId();
        verify(ruleItemTypeProvider, times(1)).findRuleItemTypeDescriptor();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void findItemTypeDescriptor_should_throw_when_no_provider_for_given_type() {
        when(ruleItemTypeProvider.getRuleItemTypeId()).thenReturn("OTHER");

        assertThatThrownBy(() -> ruleBo.findItemTypeDescriptor("UNIT"))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageStartingWith("No item type");
        verify(ruleItemTypeProvider, never()).findRuleItemTypeDescriptor();
    }

    @Test
    void findTypeDescriptor_should_work() {
        var type = "UNIT_CAPTURE";
        var expectedResult = givenRuleTypeDescriptorDto();
        when(ruleTypeProvider.getRuleTypeId()).thenReturn(type);
        when(ruleTypeProvider.findRuleTypeDescriptor()).thenReturn(expectedResult);

        var result = ruleBo.findTypeDescriptor(type);

        verify(ruleTypeProvider, times(1)).getRuleTypeId();
        verify(ruleTypeProvider, times(1)).findRuleTypeDescriptor();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void findTypeDescriptor_should_throw_when_no_provider_for_given_type() {
        when(ruleTypeProvider.getRuleTypeId()).thenReturn("OTHER");

        assertThatThrownBy(() -> ruleBo.findTypeDescriptor("CAPTURE_UNIT"))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageStartingWith("No type");
        verify(ruleTypeProvider, never()).findRuleTypeDescriptor();
    }

    @Test
    void findExtraArg_should_work_when_arg_exists() {
        var rule = givenRule();

        var result = ruleBo.findExtraArg(rule, 1);

        assertThat(result)
                .isPresent()
                .contains(SECOND_EXTRA_ARG);
    }

    @Test
    void findExtraArg_should_return_empty_when_no_such_arg() {
        var result = ruleBo.findExtraArg(givenRule(), 30);

        assertThat(result).isNotPresent();
    }

    @Test
    void findExtraArgs_should_work() {
        assertThat(ruleBo.findExtraArgs(givenRule()))
                .hasSize(2)
                .containsAll(List.of(FIRST_EXTRA_ARG, SECOND_EXTRA_ARG));
    }

    @Test
    void hasExtraArg_should_work() {
        var rule = givenRule();

        assertThat(ruleBo.hasExtraArg(rule, 0)).isTrue();
        assertThat(ruleBo.hasExtraArg(rule, 1)).isTrue();
        assertThat(ruleBo.hasExtraArg(rule, 2)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            UNIT_TYPE_ID + ",true",
            "1726617,false"
    })
    void isWantedDestination_should_return_correct_when_it_is_by_target_unit_type(int ruleDestinationId, boolean expectation) {
        var unitToCheck = givenUnit1();
        var unitType = givenEntity();
        var ruleDto = buildRuleDto("UNIT_TYPE", ruleDestinationId);
        AtomicReference<Boolean> lambdaResult = new AtomicReference<>();

        doAnswer(generateAnswer(
                unitType,
                () -> lambdaResult.get() ? Optional.of(unitType) : Optional.empty(),
                lambdaResult::set
        ))
                .when(unitTypeInheritanceFinderService).findUnitTypeMatchingCondition(eq(unitToCheck.getType()), any());

        var result = ruleBo.isWantedUnitDestination(ruleDto, unitToCheck);

        assertThat(result).isEqualTo(expectation);
        verify(unitTypeInheritanceFinderService, times(1)).findUnitTypeMatchingCondition(eq(unitType), any());
        assertThat(lambdaResult.get()).isEqualTo(expectation);
    }

    @ParameterizedTest
    @CsvSource({
            UNIT_ID_1 + ",true",
            "1726617,false"
    })
    void isWantedUnitDestination_should_return_correct_when_it_is_by_target_unit(int ruleDestinationId, boolean expectation) {
        var ruleDto = buildRuleDto(ObjectEnum.UNIT.name(), ruleDestinationId);
        var unit = givenUnit1();

        var result = ruleBo.isWantedUnitDestination(ruleDto, unit);

        assertThat(result).isEqualTo(expectation);
        verify(unitTypeInheritanceFinderService, never()).findUnitTypeMatchingCondition(any(UnitType.class), any());
    }

    @Test
    void isWantedUnitDestination_should_return_false_when_destination_is_not_valid(CapturedOutput capturedOutput) {
        var ruleDto = buildRuleDto("invalid", 12);

        assertThat(ruleBo.isWantedUnitDestination(ruleDto, givenUnit1())).isFalse();

        assertThat(capturedOutput.getOut())
                .contains("is not wanted destination for rule");
    }

    @ParameterizedTest
    @CsvSource({
            "notWantedType,false",
            TYPE + ",true"
    })
    void isWantedType(String type, boolean expectation) {
        assertThat(ruleBo.isWantedType(givenRuleDto(), type)).isEqualTo(expectation);
    }

    private RuleDto buildRuleDto(String destinationType, long destinationId) {
        return RuleDto.builder()
                .destinationType(destinationType)
                .destinationId(destinationId)
                .build();
    }

    private Answer<Optional<UnitType>> generateAnswer(UnitType unitType, Supplier<Optional<UnitType>> result, Consumer<Boolean> lambdaResult) {
        return invocationOnMock -> {
            var unitTypeFinderPredicate = new InvokePredicateLambdaAnswer<>(1, unitType);
            lambdaResult.accept(unitTypeFinderPredicate.answer(invocationOnMock));
            return result.get();
        };
    }
}
