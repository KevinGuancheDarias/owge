package com.kevinguanchedarias.owgejava.rest.open;

import com.kevinguanchedarias.owgejava.business.SponsorBo;
import com.kevinguanchedarias.owgejava.dto.SponsorDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

/**
 * The type Sponsor rest service.
 *
 * @since 0.9.21
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@RequestMapping("/open/sponsor")
@ApplicationScope
@AllArgsConstructor
public class SponsorRestService {
    private final SponsorBo sponsorBo;

    @GetMapping
    public List<SponsorDto> findAll() {
        return sponsorBo.toDto(sponsorBo.findAll());
    }
}
