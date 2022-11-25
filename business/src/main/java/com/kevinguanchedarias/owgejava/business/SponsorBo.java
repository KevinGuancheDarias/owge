package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.SponsorDto;
import com.kevinguanchedarias.owgejava.entity.Sponsor;
import com.kevinguanchedarias.owgejava.repository.SponsorRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.io.Serial;

/**
 * The type Sponsor bo.
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.21
 */
@Service
@AllArgsConstructor
public class SponsorBo implements WithNameBo<Integer, Sponsor, SponsorDto> {
    public static final String SPONSOR_CACHE_TAG = "sponsor";

    @Serial
    private static final long serialVersionUID = 8446895252128116076L;

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
