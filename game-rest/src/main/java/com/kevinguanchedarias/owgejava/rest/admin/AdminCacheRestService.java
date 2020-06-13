package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;

/**
 * Temporary allows to drop all cache entries from admin panel
 *
 * @todo In the far future, the system should selectively drop caches
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("admin/cache")
@ApplicationScope
public class AdminCacheRestService {
	@Autowired
	private ImprovementBo improvementBo;

	@Autowired
	private SocketIoService socketIoService;

	/**
	 *
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@DeleteMapping("drop-all")
	public void dropAll() {
		improvementBo.getImprovementSources().forEach(current -> improvementBo.clearCacheEntries(current));
		socketIoService.clearCache();
	}
}
