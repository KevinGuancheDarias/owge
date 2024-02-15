package com.kevinguanchedarias.owgejava.logback.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class IgnoreDbSchedulersLogbackFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent event) {
        return event.getLoggerName().startsWith("jdbc.")
                && (event.getMessage().startsWith(" com.github.kagkarlsson.jdbc.JdbcRunner.lambda")
                || event.getMessage().startsWith(" org.quartz.impl.jdbcjobstore"))
                ? FilterReply.DENY
                : FilterReply.NEUTRAL;
    }
}
