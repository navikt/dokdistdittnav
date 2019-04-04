package no.nav.dokdistdittnav.metrics;

import static no.nav.dokdistdittnav.qdist010.Qdist010Route.SERVICE_ID;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdisttnavFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.support.RoutePolicySupport;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Component
public class Qdist010MetricsRoutePolicy extends RoutePolicySupport {

	private final MeterRegistry registry;
	private Timer.Sample timer;

	private static final String QDIST010_PROCESS_TIMER = "dok_request_latency";
	private static final String QDIST010_PROCESS_TIMER_DESCRIPTION = "prosesseringstid for kall inn til qdist010";
	private static final String QDIST010_EXCEPTION = "request_exception_total";

	@Inject
	public Qdist010MetricsRoutePolicy(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void onExchangeBegin(Route route, Exchange exchange) {
		timer = Timer.start(registry);
	}

	@Override
	public void onExchangeDone(Route route, Exchange exchange) {
		Exception exception = getException(exchange);

		timer.stop(Timer.builder(QDIST010_PROCESS_TIMER)
				.description(QDIST010_PROCESS_TIMER_DESCRIPTION)
				.tags(MetricLabels.LABEL_PROCESS, SERVICE_ID)
				.publishPercentileHistogram(true)
				.register(registry));

		if (exception != null) {
			if (isFunctionalException(exception)) {
				registry.counter(QDIST010_EXCEPTION,
						MetricLabels.LABEL_ERROR_TYPE, MetricLabels.TYPE_FUNCTIONAL_EXCEPTION,
						MetricLabels.LABEL_EXCEPTION_NAME, exception.getClass().getSimpleName(),
						MetricLabels.LABEL_PROCESS, SERVICE_ID).increment();
			} else {
				registry.counter(QDIST010_EXCEPTION,
						MetricLabels.LABEL_ERROR_TYPE, MetricLabels.TYPE_TECHNICAL_EXCEPTION,
						MetricLabels.LABEL_EXCEPTION_NAME, exception.getClass().getCanonicalName(),
						MetricLabels.LABEL_PROCESS, SERVICE_ID).increment();
			}
		}
	}

	private boolean isFunctionalException(Exception e) {
		return (e instanceof AbstractDokdistdisttnavFunctionalException);
	}

	private Exception getException(Exchange exchange) {
		Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		if (exception == null && exchange.getException() instanceof Exception) {
			exception = (Exception) exchange.getException().getCause();
		}
		return exception;
	}
}
