package com.kevinguanchedarias.owgejava.logback.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Due to Hibernate internally throwing an exception which is caught by the jdbc proxy, and then logged as a real exception
 */
public class HibernateBugWorkaroundLogbackFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        var loggerName = iLoggingEvent.getLoggerName();
        if (iLoggingEvent.getLevel().equals(Level.ERROR)
                && ("jdbc.audit".equals(loggerName) || "jdbc.sqlonly".equals(loggerName) || "jdbc.sqltiming".equals(loggerName))
                && iLoggingEvent.getMessage().contains("PreparedStatement.getMaxRows()")
                && iLoggingEvent.getThrowableProxy().getMessage().startsWith("No operations allowed after")) {
            return FilterReply.DENY;
        } else {
            return FilterReply.NEUTRAL;
        }
    }
}
