/**
 *
 */
package com.kevinguanchedarias.owgejava.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * Has the config required to connect to SQS
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Component
public class SqsConfiguration {

    @Value("${OWGE_SQS_HOST:127.0.0.1}")
    private String host;

    @Value("${OWGE_SQS_PORT:7474}")
    private Integer port;

    @Value("${OWGE_SQS_QUEUE:unused}")
    private String queue;

    /**
     * @return the host
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port the port to set
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * @return the queue
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public String getQueue() {
        return queue;
    }

    /**
     * @param queue the queue to set
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void setQueue(String queue) {
        this.queue = queue;
    }

}
