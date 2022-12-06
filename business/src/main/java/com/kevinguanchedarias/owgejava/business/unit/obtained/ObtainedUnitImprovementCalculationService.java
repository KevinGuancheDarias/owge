package com.kevinguanchedarias.owgejava.business.unit.obtained;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.ImprovementSource;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@AllArgsConstructor
public class ObtainedUnitImprovementCalculationService implements ImprovementSource {

    private final ObtainedUnitRepository repository;
    private final ImprovementBo improvementBo;

    @PostConstruct
    public void init() {
        improvementBo.addImprovementSource(this);
    }

    @Override
    public GroupedImprovement calculateImprovement(UserStorage user) {
        var groupedImprovement = new GroupedImprovement();
        repository.findByUserAndNotBuilding(user.getId()).forEach(current -> groupedImprovement.add(current.getUnit().getImprovement()));
        return groupedImprovement;
    }
}
