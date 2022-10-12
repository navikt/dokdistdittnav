package no.nav.dokdistdittnav.kdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.kafka.CamelKafkaProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.utils.MDCProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.consumer.DefaultKafkaManualCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.constants.DomainConstants.KDIST001_ID;
import static org.apache.camel.component.kafka.KafkaConstants.MANUAL_COMMIT;

@Slf4j
@Component
public class Kdist001Route extends RouteBuilder {

	private final CamelKafkaProperties camelKafkaProperties;
	private final Ferdigprodusent ferdigprodusent;
	private final DokdistdittnavProperties dokdistdittnavProperties;

	@Autowired
	public Kdist001Route(CamelKafkaProperties camelKafkaProperties, Ferdigprodusent ferdigprodusent,
						 DokdistdittnavProperties dokdistdittnavProperties) {
		this.camelKafkaProperties = camelKafkaProperties;
		this.ferdigprodusent = ferdigprodusent;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
	}

	@Override
	public void configure() {
		errorHandler(defaultErrorHandler()
				.onExceptionOccurred(exchange -> {
					Throwable exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
					if (exception != null && !(exception instanceof AbstractDokdistdittnavFunctionalException)) {
						DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
						manual.getConsumer().seek(manual.getPartition(), manual.getRecordOffset());
						log.error("Kdist001 Teknisk feil. Seek tilbake til record(topic={}, partition={}, offset={})", manual.getTopicName(),
								manual.getPartition().partition(), manual.getRecordOffset());
					}
				})
				.retryAttemptedLogLevel(LoggingLevel.ERROR)
				.logRetryStackTrace(false)
				.logExhaustedMessageBody(false)
				.loggingLevel(LoggingLevel.ERROR));

		onException(AbstractDokdistdittnavFunctionalException.class)
				.handled(true)
				.maximumRedeliveries(0)
				.logExhaustedMessageBody(false)
				.logExhaustedMessageHistory(false)
				.logStackTrace(false)
				.logRetryAttempted(false)
				.process(exchange -> {
					DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
					if (manual != null) {
						log.error("Kdist001 Funksjonell feil i record" + defaultKafkaManualCommit(exchange));
						manual.commit();
					}
				})
				.log(LoggingLevel.WARN, log, "${exception}");


		from(camelKafkaProperties.buildKafkaUrl(dokdistdittnavProperties.getTopic().getLestavmottaker(), camelKafkaProperties.kafkaConsumer()))
				.autoStartup(dokdistdittnavProperties.isAutostartup())
				.id(KDIST001_ID)
				.process(new MDCProcessor())
				.process(exchange -> log.info("Kdist001 mottatt " + defaultKafkaManualCommit(exchange)))
				.bean(ferdigprodusent)
				.process(exchange -> {
					DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
					if (manual != null) {
						log.info("Kdist001, manual commit " + defaultKafkaManualCommit(exchange));
						manual.commit();
					}
				})
				.end();


	}

	private String defaultKafkaManualCommit(Exchange exchange) {
		DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		return format("(topic=%s, partition={%s, offset=%s, groupId=%s).", manual.getTopicName(), manual.getPartition().partition(), manual.getRecordOffset(), camelKafkaProperties.getGroupId());
	}
}
