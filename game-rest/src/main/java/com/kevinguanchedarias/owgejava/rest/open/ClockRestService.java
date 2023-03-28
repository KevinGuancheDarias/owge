/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.open;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Date;

/**
 * Used to sync the browser and the server time
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.3
 */
@RestController
@RequestMapping("/open/clock")
@ApplicationScope
public class ClockRestService {

    /**
     * Returns the current time of the server
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.3
     */
    @GetMapping()
    public Date currentTime() {
        return new Date();
    }
}
