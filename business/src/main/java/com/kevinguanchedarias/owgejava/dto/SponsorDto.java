package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Sponsor;
import com.kevinguanchedarias.owgejava.enumerations.SponsorTypeEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * The type Sponsor dto.
 *
 * @since 0.9.21
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Getter
@Setter
public class SponsorDto extends  CommonDtoWithImageStore<Integer, Sponsor> {
    private String url;
    private SponsorTypeEnum type;
}
