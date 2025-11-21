package no.nav.dokdistdittnav.kdist002;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBContext;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.kafka.CamelKafkaProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.kafka.InfoForMinSideOgPrintForsendelse;
import no.nav.dokdistdittnav.utils.MDCProcessor;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.consumer.DefaultKafkaManualCommit;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistdittnav.constants.DomainConstants.KDIST002_ID;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;
import static org.apache.camel.component.kafka.KafkaConstants.MANUAL_COMMIT;

@Slf4j
@Component
public class Kdist002Route extends RouteBuilder {

	private static final String QDIST009 = "qdist009";
	private static final String SEND_INAKTIVER_VARSEL_EVENT = "inaktiver_varsel";
	public static final String TMS_EKSTERN_VARSLING = "tms-ekstern-varsling";

	public static final String PROPERTY_MINSIDE_BESTILLINGS_ID = "minSideBestillingsId";
	public static final String PROPERTY_MINSIDE_FORSENDELSE_ID = "minSideForsendelseId";
	public static final String PROPERTY_PRINT_BESTILLINGS_ID = "printBestillingsId";
	public static final String PROPERTY_PRINT_FORSENDELSE_ID = "printForsendelseId";

	private final CamelKafkaProperties camelKafkaProperties;
	private final Kdist002Service kdist002Service;
	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final Queue qdist009;
	private final InaktiverVarselService inaktiverVarselService;

	public Kdist002Route(CamelKafkaProperties camelKafkaProperties,
						 Kdist002Service kdist002Service,
						 DokdistdittnavProperties dokdistdittnavProperties,
						 Queue qdist009,
						 InaktiverVarselService inaktiverVarselService) {
		this.camelKafkaProperties = camelKafkaProperties;
		this.kdist002Service = kdist002Service;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
		this.qdist009 = qdist009;
		this.inaktiverVarselService = inaktiverVarselService;
	}

	@Override
	public void configure() throws Exception {

		//@formatter:off
		errorHandler(defaultErrorHandler()
				.onExceptionOccurred(exchange -> {
					Throwable exception = exchange.getProperty(EXCEPTION_CAUGHT, Throwable.class);
					if (exception != null && !(exception instanceof AbstractDokdistdittnavFunctionalException)) {
						lesInnMeldingPaaNytt(exchange, exception);
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
				.process(this::avsluttProsesseringAvMelding)
				.log(WARN, log, "${exception}");

		from(camelKafkaProperties.buildKafkaUrl(dokdistdittnavProperties.getDoknotifikasjon().getStatustopic(), camelKafkaProperties.kafkaConsumer()))
				.autoStartup(dokdistdittnavProperties.isAutostartup())
				.id(KDIST002_ID)
				.process(new MDCProcessor())
				.choice()
					.when(harFeilBestillerId())
						.process(this::avsluttProsesseringAvMelding)
					.otherwise()
						.bean(kdist002Service)
						.choice()
							.when(simple("${body}").isNull())
								.process(this::avsluttProsesseringAvMelding)
							.otherwise()
								.process(exchange -> settForsendelseOgBestillingsIdProperties(exchange, exchange.getIn().getBody(InfoForMinSideOgPrintForsendelse.class)))
								.multicast()
									.to("direct:" + QDIST009)
									.to("direct:" + SEND_INAKTIVER_VARSEL_EVENT)
								.end()
						.end()
				.end()
				.process(this::avsluttProsesseringAvMelding);

		from("direct:" + QDIST009)
				.id(QDIST009)
				.setBody(exchangeProperty(PROPERTY_PRINT_FORSENDELSE_ID))
				.process(exchange -> {
					DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();
					distribuerTilKanal.setForsendelseId(exchange.getIn().getBody(String.class));
					exchange.getIn().setBody(distribuerTilKanal);
				})
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.convertBodyTo(String.class, UTF_8.toString())
				.to("jms:" + qdist009.getQueueName())
				.log(INFO, log, "kdist002 har skrevet forsendelse med %s på kø til qdist009.".formatted(getIdsForLoggingPrint()))
				.end();

		from("direct:" + SEND_INAKTIVER_VARSEL_EVENT)
				.id(SEND_INAKTIVER_VARSEL_EVENT)
				.setBody(exchangeProperty(PROPERTY_MINSIDE_BESTILLINGS_ID))
				.bean(inaktiverVarselService, "sendInaktiverVarselEventTilMinSide")
				.log(INFO, "kdist002 har skrevet hendelse med %s til topic=%s".formatted(getIdsForLoggingMinSide(), dokdistdittnavProperties.getMinside().getVarseltopic()))
				.end();
		//@formatter:on
	}

	private Predicate harFeilBestillerId() {
		return PredicateBuilder.and(
				simple("${body.bestillerId}").isNotEqualTo(dokdistdittnavProperties.getAppnavn()),
				simple("${body.bestillerId}").isNotEqualTo(TMS_EKSTERN_VARSLING)
		);
	}

	private void settForsendelseOgBestillingsIdProperties(Exchange exchange, InfoForMinSideOgPrintForsendelse infoForMinSideOgPrintForsendelse) {
		exchange.setProperty(PROPERTY_MINSIDE_BESTILLINGS_ID, infoForMinSideOgPrintForsendelse.getMinSideBestillingsId());
		exchange.setProperty(PROPERTY_MINSIDE_FORSENDELSE_ID, infoForMinSideOgPrintForsendelse.getMinSideForsendelseId());
		exchange.setProperty(PROPERTY_PRINT_BESTILLINGS_ID, infoForMinSideOgPrintForsendelse.getPrintBestillingsId());
		exchange.setProperty(PROPERTY_PRINT_FORSENDELSE_ID, infoForMinSideOgPrintForsendelse.getPrintForsendelseId());
	}

	private static String getIdsForLoggingMinSide() {
		return "minSideBestillingsId=${exchangeProperty." + PROPERTY_MINSIDE_BESTILLINGS_ID + "}, " +
			   "minSideForsendelseId=${exchangeProperty." + PROPERTY_MINSIDE_FORSENDELSE_ID + "}";
	}

	private static String getIdsForLoggingPrint() {
		return "printBestillingsId=${exchangeProperty." + PROPERTY_PRINT_BESTILLINGS_ID + "}, " +
			   "printForsendelseId=${exchangeProperty." + PROPERTY_PRINT_FORSENDELSE_ID + "}";
	}

	private void avsluttProsesseringAvMelding(Exchange exchange) {
		DefaultKafkaManualCommit manualCommit = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		if (manualCommit != null) {
			if (log.isDebugEnabled()) {
				log.debug("kdist002 manual commit {}", formaterKafkaMetadata(manualCommit));
			}
			manualCommit.commit();
		}
	}

	private void lesInnMeldingPaaNytt(Exchange exchange, Throwable exception) {
		DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		manual.getCamelExchangePayload().consumer.seek(manual.getPartition(), manual.getRecordOffset());

		log.error("kdist002 teknisk feil. Seek tilbake til record(topic={}, partition={}, offset={})",
				manual.getTopicName(), manual.getPartition().partition(), manual.getRecordOffset(), exception);
	}

	private String formaterKafkaMetadata(DefaultKafkaManualCommit manualCommit) {
		return format("(topic=%s, partition={%s, offset=%s, groupId=%s).", manualCommit.getTopicName(), manualCommit.getPartition().partition(), manualCommit.getRecordOffset(), camelKafkaProperties.getGroupId());
	}
}
