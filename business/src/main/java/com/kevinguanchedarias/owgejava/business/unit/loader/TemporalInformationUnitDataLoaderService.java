package com.kevinguanchedarias.owgejava.business.unit.loader;

import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor
public class TemporalInformationUnitDataLoaderService implements UnitDataLoader {

    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;

    @Override
    public void addInformationToDto(ObtainedUnit obtainedUnit, ObtainedUnitDto targetDto) {
        var expirationId = obtainedUnit.getExpirationId();
        if (expirationId != null) {
            var temporalInformationOpt = obtainedUnitTemporalInformationRepository.findById(expirationId);
            temporalInformationOpt.ifPresent(temporalInformation -> {
                temporalInformation.setPendingMillis(
                        ChronoUnit.MILLIS.between(Instant.now(), temporalInformation.getExpiration())
                );
                targetDto.setTemporalInformation(temporalInformation);
            });
        }
    }
}
