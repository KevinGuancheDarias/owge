package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/faction")
@ApplicationScope
@AllArgsConstructor
public class OpenFactionsRestService {

    private final FactionBo factionBo;

    @GetMapping
    @Transactional
    public List<FactionDto> all() {
        return factionBo.toDto(factionBo.findVisible(true));
    }
}
