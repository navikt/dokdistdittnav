/*
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.nav.dokdistdittnav.metrics;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.function.Function;


@Aspect
@Incubating(since = "1.0.0")
@Slf4j
@SuppressWarnings("Duplicates")
public class DokMonitoringAspect {
	private final MeterRegistry registry;
	private final Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint;

	public DokMonitoringAspect(MeterRegistry registry) {
		this(registry, pjp ->
				Tags.of("class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
						"method", pjp.getStaticPart().getSignature().getName())
		);
	}

	private DokMonitoringAspect(MeterRegistry registry, Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint) {
		this.registry = registry;
		this.tagsBasedOnJoinpoint = tagsBasedOnJoinpoint;
	}

	@Around("execution (@no.nav.dokdistdittnav.metrics.Monitor * *.*(..))")
	public Object dokMetrics(ProceedingJoinPoint pjp) throws Throwable {
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		Monitor monitor = method.getAnnotation(Monitor.class);

		if (monitor.value().isEmpty()) {
			return pjp.proceed();
		}

		Timer.Sample sample = Timer.start(registry);
		try {
			return pjp.proceed();
		} finally {
			sample.stop(Timer.builder(monitor.value())
					.description(monitor.description().isEmpty() ? null : monitor.description())
					.tags(monitor.extraTags())
					.tags(tagsBasedOnJoinpoint.apply(pjp))
					.publishPercentileHistogram(monitor.histogram())
					.publishPercentiles(monitor.percentiles().length == 0 ? null : monitor.percentiles())
					.register(registry));
		}
	}

}
