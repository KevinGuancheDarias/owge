package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.CriticalAttackDto;
import com.kevinguanchedarias.owgejava.dto.CriticalAttackEntryDto;
import com.kevinguanchedarias.owgejava.entity.CriticalAttack;
import com.kevinguanchedarias.owgejava.entity.CriticalAttackEntry;
import com.kevinguanchedarias.owgejava.repository.CriticalAttackEntryRepository;
import com.kevinguanchedarias.owgejava.repository.CriticalAttackRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CriticalAttackBo implements  BaseReadBo<Integer, CriticalAttack> {

    private final CriticalAttackRepository repository;
    private final CriticalAttackEntryRepository entriesRepository;

    @Override
    public JpaRepository<CriticalAttack, Integer> getRepository() {
        return repository;
    }

    @Transactional
    public CriticalAttack save(CriticalAttackDto dto) {
        var entity = dtoToEntity(dto);
        if(entity.getId() != null) {
            entriesRepository.deleteByCriticalAttackId(entity.getId());
        }
        var saved = repository.save(entity);
        List<CriticalAttackEntry> entries = new ArrayList<>();
        saved.setEntries(entries);
        dto.getEntries().forEach(entryDto -> {
            var entryEntity = entryDtoToEntity(entryDto);
            if(entryEntity.getValue() < 0) {
                entryEntity.setValue(Math.abs(entryEntity.getValue()));
            }
            entryEntity.setCriticalAttack(entity);
            entries.add(entriesRepository.save(entryEntity));
        });
        return saved;
    }

    @Transactional
    public void delete(Integer criticalId) {
        delete(findByIdOrDie(criticalId));
    }

    @Transactional
    public void delete(CriticalAttack critical) {
        entriesRepository.deleteAll(critical.getEntries());
        repository.delete(critical);
    }

    public CriticalAttackDto toDto(CriticalAttack criticalAttack) {
        return CriticalAttackDto.builder()
                .id(criticalAttack.getId())
                .name(criticalAttack.getName())
                .entries(criticalAttack.getEntries().stream()
                        .map(entry -> CriticalAttackEntryDto.builder()
                            .id(entry.getId())
                                .referenceId(entry.getReferenceId())
                                .target(entry.getTarget())
                                .value(entry.getValue())
                                .build()
                        ).collect(Collectors.toList())
                )
                .build();
    }

    private CriticalAttack dtoToEntity(CriticalAttackDto dto) {
        return CriticalAttack.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }

    private CriticalAttackEntry entryDtoToEntity(CriticalAttackEntryDto dto) {
        return CriticalAttackEntry.builder()
                .id(dto.getId())
                .target(dto.getTarget())
                .referenceId(dto.getReferenceId())
                .value(dto.getValue())
                .build();
    }


}
