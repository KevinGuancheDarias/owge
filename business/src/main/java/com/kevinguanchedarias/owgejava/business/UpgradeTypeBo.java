package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.UpgradeTypeDto;
import com.kevinguanchedarias.owgejava.entity.UpgradeType;
import com.kevinguanchedarias.owgejava.repository.UpgradeTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.io.Serial;

@Service
@AllArgsConstructor
public class UpgradeTypeBo implements WithNameBo<Integer, UpgradeType, UpgradeTypeDto> {
    public static final String UPGRADE_TYPE_CACHE_TAG = "upgrade_type";
    @Serial
    private static final long serialVersionUID = 84836919835815466L;

    private final UpgradeTypeRepository upgradeTypeRepository;

    @Override
    public JpaRepository<UpgradeType, Integer> getRepository() {
        return upgradeTypeRepository;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<UpgradeTypeDto> getDtoClass() {
        return UpgradeTypeDto.class;
    }

}