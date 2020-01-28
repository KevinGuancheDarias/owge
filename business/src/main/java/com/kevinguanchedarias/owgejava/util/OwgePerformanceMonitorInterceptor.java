package com.kevinguanchedarias.owgejava.util;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.springframework.aop.interceptor.PerformanceMonitorInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StopWatch;

public class OwgePerformanceMonitorInterceptor extends PerformanceMonitorInterceptor {
	private static final long serialVersionUID = 2421454502195362331L;

	@Value("${OWGE_PERFORMANCE_WARNING_MILLIS:5}")
	private long logAsWarningMillis;

	public OwgePerformanceMonitorInterceptor(boolean useDynamicLogger) {
		super(useDynamicLogger);
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Log logger = getLoggerForInvocation(invocation);
		String name = createInvocationTraceName(invocation);
		StopWatch stopWatch = new StopWatch(name);
		stopWatch.start(name);
		try {
			return invocation.proceed();
		} finally {
			stopWatch.stop();
			String shortSummary = stopWatch.shortSummary();
			if (stopWatch.getTotalTimeMillis() > logAsWarningMillis) {
				logger.warn(shortSummary);
			} else {
				writeToLog(logger, shortSummary);
			}
		}
	}
}
