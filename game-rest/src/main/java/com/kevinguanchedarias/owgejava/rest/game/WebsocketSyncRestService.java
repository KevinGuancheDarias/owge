package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.WebsocketSyncService;

/**
 *
 * @since 0.9.6
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("game/websocket-sync")
@ApplicationScope
public class WebsocketSyncRestService {
	@Autowired
	private WebsocketSyncService websocketSyncService;

	/**
	 *
	 * @param keys
	 * @return
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping
	public Map<String, Object> sync(@RequestParam List<String> keys) {
		return websocketSyncService.findWantedData(keys);
	}

}
