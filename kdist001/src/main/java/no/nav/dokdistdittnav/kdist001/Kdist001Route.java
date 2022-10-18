package no.nav.dokdistdittnav.kdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.kafka.CamelKafkaProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.metrics.DittnavMetricsRoutePolicy;
import no.nav.dokdistdittnav.utils.MDCProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.consumer.DefaultKafkaManualCommit;
import org.apache.camel.spi.RoutePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.constants.DomainConstants.KDIST001_ID;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.component.kafka.KafkaConstants.MANUAL_COMMIT;

@Slf4j
@Component
public class Kdist001Route extends RouteBuilder {

	private final CamelKafkaProperties camelKafkaProperties;
	private final Ferdigprodusent ferdigprodusent;
	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final DittnavMetricsRoutePolicy metricsRoutePolicy;

	@Autowired
	public Kdist001Route(CamelKafkaProperties camelKafkaProperties, Ferdigprodusent ferdigprodusent,
						 DokdistdittnavProperties dokdistdittnavProperties,DittnavMetricsRoutePolicy metricsRoutePolicy) {
		this.camelKafkaProperties = camelKafkaProperties;
		this.ferdigprodusent = ferdigprodusent;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
		this.metricsRoutePolicy = metricsRoutePolicy;
	}

	@Override
	public void configure() {

		//@formatter:off
		errorHandler(defaultErrorHandler()
				.onExceptionOccurred(exchange -> {
					Throwable exception = exchange.getProperty(EXCEPTION_CAUGHT, Throwable.class);
					if (exception != null && !(exception instanceof AbstractDokdistdittnavFunctionalException)) {
						DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
						manual.getConsumer().seek(manual.getPartition(), manual.getRecordOffset());
						log.error("Kdist001 Teknisk feil. Seek tilbake til record(topic={}, partition={}, offset={})", manual.getTopicName(),
								manual.getPartition().partition(), manual.getRecordOffset());
					}
				})
				.retryAttemptedLogLevel(ERROR)
				.logRetryStackTrace(false)
				.logExhaustedMessageBody(false)
				.loggingLevel(ERROR));

		onException(AbstractDokdistdittnavFunctionalException.class)
				.handled(true)
				.maximumRedeliveries(0)
				.logExhaustedMessageBody(false)
				.logExhaustedMessageHistory(false)
				.logStackTrace(false)
				.logRetryAttempted(false)
				.process(this::defaultKafkaManualCommit)
				.log(LoggingLevel.WARN, log, "${exception}");

		from(camelKafkaProperties.buildKafkaUrl(dokdistdittnavProperties.getTopic().getLestavmottaker(), camelKafkaProperties.kafkaConsumer()))
				.autoStartup(dokdistdittnavProperties.isAutostartup())
				.id(KDIST001_ID)
				.routePolicy(metricsRoutePolicy)
				.process(new MDCProcessor())
				.process(exchange -> log.info("Kdist001 mottatt " + createLoggingFraHeader(exchange)))
				.bean(ferdigprodusent)
				.process(this::defaultKafkaManualCommit)
				.end();
		//@formatter:on
	}

	private void defaultKafkaManualCommit(Exchange exchange) {
		DefaultKafkaManualCommit manualCommit = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		if (manualCommit != null) {
			log.info("Kdist001, manual commit " + createLogging(manualCommit));
			manualCommit.commit();
		}
	}

	private String createLoggingFraHeader(Exchange exchange) {
		DefaultKafkaManualCommit manualCommit = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		return createLogging(manualCommit);
	}

	private String createLogging(DefaultKafkaManualCommit manualCommit) {
		return format("(topic=%s, partition={%s, offset=%s, groupId=%s).", manualCommit.getTopicName(), manualCommit.getPartition().partition(), manualCommit.getRecordOffset(), camelKafkaProperties.getGroupId());
	}
}
