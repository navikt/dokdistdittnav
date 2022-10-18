package no.nav.dokdistdittnav.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.ValidationException;
import org.apache.camel.support.RoutePolicySupport;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;

import static no.nav.dokdistdittnav.constants.DomainConstants.SERVICE_ID;
import static no.nav.dokdistdittnav.metrics.MetricLabels.LABEL_ERROR_TYPE;
import static no.nav.dokdistdittnav.metrics.MetricLabels.LABEL_EXCEPTION_NAME;
import static no.nav.dokdistdittnav.metrics.MetricLabels.LABEL_PROCESS;
import static no.nav.dokdistdittnav.metrics.MetricLabels.TYPE_FUNCTIONAL_EXCEPTION;
import static no.nav.dokdistdittnav.metrics.MetricLabels.TYPE_TECHNICAL_EXCEPTION;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Component
public class DittnavMetricsRoutePolicy extends RoutePolicySupport {

	private final MeterRegistry registry;
	private Timer.Sample timer;

	private static final String QDIST010_PROCESS_TIMER = "dok_request_latency";
	private static final String QDIST010_PROCESS_TIMER_DESCRIPTION = "prosesseringstid for kall inn til qdist010";
	private static final String QDIST010_EXCEPTION = "dok_request_exception_total";

	@Autowired
	public DittnavMetricsRoutePolicy(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void onExchangeBegin(Route route, Exchange exchange) {
		timer = Timer.start(registry);
	}

	@Override
	public void onExchangeDone(Route route, Exchange exchange) {
		Exception exception = getException(exchange);
		String routeId = exchange.getFromRouteId();

		timer.stop(Timer.builder(QDIST010_PROCESS_TIMER)
				.description(QDIST010_PROCESS_TIMER_DESCRIPTION)
				.tags(LABEL_PROCESS, SERVICE_ID)
				.publishPercentileHistogram(true)
				.register(registry));

		if (exception != null) {
			if (isFunctionalException(exception)) {
				registry.counter(QDIST010_EXCEPTION,
						LABEL_ERROR_TYPE, TYPE_FUNCTIONAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getSimpleName(),
						LABEL_PROCESS, routeId).increment();
			} else {
				registry.counter(QDIST010_EXCEPTION,
						LABEL_ERROR_TYPE, TYPE_TECHNICAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getCanonicalName(),
						LABEL_PROCESS, routeId).increment();
			}
		}
	}

	private boolean isFunctionalException(Exception e) {
		return (e instanceof AbstractDokdistdittnavFunctionalException ||
				e instanceof JAXBException ||
				e instanceof ValidationException);
	}

	private Exception getException(Exchange exchange) {
		Exception exception = exchange.getProperty(EXCEPTION_CAUGHT, Exception.class);
		if (exception == null && exchange.getException() instanceof Exception) {
			exception = (Exception) exchange.getException().getCause();
		}
		return exception;
	}
}
