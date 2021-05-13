package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.business.CriticalAttackBo;
import com.kevinguanchedarias.owgejava.dto.CriticalAttackDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

@RestController
@ApplicationScope
@RequestMapping("admin/critical-attack")
@AllArgsConstructor
public class AdminCriticalAttackRestService {

    private final CriticalAttackBo criticalAttackBo;

    @PostMapping
    public CriticalAttackDto save(@RequestBody CriticalAttackDto body) {
        return criticalAttackBo.toDto(criticalAttackBo.save(body));
    }

    /**
     *
     * @since 0.9.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @PutMapping("{id}")
    public CriticalAttackDto save(@PathVariable Integer id, @RequestBody CriticalAttackDto body) {
        body.setId(id);
        return save(body);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable Integer id) {
        criticalAttackBo.delete(id);
    }
}
