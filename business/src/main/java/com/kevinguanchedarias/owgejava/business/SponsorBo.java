package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.SponsorDto;
import com.kevinguanchedarias.owgejava.entity.Sponsor;
import com.kevinguanchedarias.owgejava.repository.SponsorRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * The type Sponsor bo.
 *
 * @since 0.9.21
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
@AllArgsConstructor
public class SponsorBo implements  WithNameBo<Integer, Sponsor, SponsorDto> {

    private final transient SponsorRepository sponsorRepository;

    @Override
    public JpaRepository<Sponsor, Integer> getRepository() {
        return sponsorRepository;
    }

    @Override
    public Class<SponsorDto> getDtoClass() {
        return SponsorDto.class;
    }
}
