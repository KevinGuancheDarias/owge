package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

/**
 * Temporary allows to drop all cache entries from admin panel
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @todo In the far future, the system should selectively drop caches
 * @since 0.9.0
 */
@RestController
@RequestMapping("admin/cache")
@ApplicationScope
@AllArgsConstructor
public class AdminCacheRestService {
    private final ImprovementBo improvementBo;
    private final SocketIoService socketIoService;
    private final RequirementInformationDao requirementInformationDao;
    private final TaggableCacheManager taggableCacheManager;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @DeleteMapping("drop-all")
    public void dropAll() {
        requirementInformationDao.clearCache();
        improvementBo.getImprovementSources().forEach(improvementBo::clearCacheEntries);
        socketIoService.clearCache();
        taggableCacheManager.clear();
    }
}
