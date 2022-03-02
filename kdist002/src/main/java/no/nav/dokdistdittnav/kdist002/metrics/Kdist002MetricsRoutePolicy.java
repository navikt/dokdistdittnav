package no.nav.dokdistdittnav.kdist002.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.ValidationException;
import org.apache.camel.support.RoutePolicySupport;

@RequiredArgsConstructor
public class Kdist002MetricsRoutePolicy extends RoutePolicySupport {

	private static final String EXCEPTION_COUNTER = "dok_metric_exception_total";
	private static final String KDIST002_PROCESS_TIMER = "dok_route_latency_histogram";
	private static final String KDIST002__PROCESS_TIMER_DESCRIPTION = "prosesseringstid for kall inn til qdist008";
	private static final String KDIST002_START = "Kdist002_start";
	public static final String SERVICE_ID = "kdist002";

	private final MeterRegistry meterRegistry;
	private Timer.Sample timer;

	@Override
	public void onExchangeBegin(Route route, Exchange exchange) {
		timer = Timer.start();
		meterRegistry.counter(KDIST002_START).increment();
	}

	@Override
	public void onExchangeDone(Route route, Exchange exchange) {
		super.onExchangeDone(route, exchange);
	}

	private boolean isFunctionalException(Exception e) {
		return (e instanceof AbstractDokdistdittnavFunctionalException) || (e instanceof ValidationException);
	}

	private Exception getException(Exchange exchange) {
		Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		if (exception == null && exchange.getException() != null) {
			exception = (Exception) exchange.getException().getCause();
		}
		return exception;
	}
}
