package no.nav.dokdistdittnav.qdist010;

import no.nav.dokdistdittnav.brukernotifikasjon.ProdusentNotifikasjon;
import no.nav.dokdistdittnav.config.alias.BrukernotifikasjonTopic;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.metrics.Qdist010MetricsRoutePolicy;
import no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.Dokumenthenvendelse;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spi.DataFormat;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.nio.charset.StandardCharsets;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.component.kafka.KafkaConstants.KEY;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class Qdist010Route extends RouteBuilder {

	public static final String SERVICE_ID = "qdist010";
	static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
	static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";
	static final String PROPERTY_VARSELBESTILLING_ID = "varselbestillingId";
	static final String PROPERTY_JOURNALPOST_ID = "journalpostId";

	private final ProdusentNotifikasjon produsentNotifikasjon;
	private final DokdistStatusUpdater dokdistStatusUpdater;
	private final Queue qdist010;
	private final Queue qdist010FunksjonellFeil;
	private final Qdist010MetricsRoutePolicy qdist010MetricsRoutePolicy;
	private final BrukernotifikasjonTopic topic;

	@Inject
	public Qdist010Route(ProdusentNotifikasjon produsentNotifikasjon, BrukernotifikasjonTopic topic,
						 DokdistStatusUpdater dokdistStatusUpdater,
						 Queue qdist010,
						 Queue qdist010FunksjonellFeil,
						 Qdist010MetricsRoutePolicy qdist010MetricsRoutePolicy) {
		this.produsentNotifikasjon = produsentNotifikasjon;
		this.topic = topic;
		this.dokdistStatusUpdater = dokdistStatusUpdater;
		this.qdist010 = qdist010;
		this.qdist010FunksjonellFeil = qdist010FunksjonellFeil;
		this.qdist010MetricsRoutePolicy = qdist010MetricsRoutePolicy;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(true)
				.loggingLevel(ERROR));

		onException(AbstractDokdistdittnavFunctionalException.class, JAXBException.class, ValidationException.class)
				.handled(true)
				.useOriginalMessage()
				.log(LoggingLevel.WARN, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist010FunksjonellFeil.getQueueName());

		from("jms:" + qdist010.getQueueName() +
				"?transacted=true")
				.routeId(SERVICE_ID)
				.routePolicy(qdist010MetricsRoutePolicy)
				.setExchangePattern(ExchangePattern.InOnly)
				.process(new IdsProcessor())
				.log(LoggingLevel.INFO, log, "qdist010 har mottatt forsendelse med forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}")
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/out/distribuertilkanal.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.process(exchange -> {
					DistribuerTilKanal distribuerTilKanal = exchange.getIn().getBody(DistribuerTilKanal.class);
					exchange.setProperty(PROPERTY_FORSENDELSE_ID, distribuerTilKanal.getForsendelseId());
				})
				.bean(produsentNotifikasjon)
				.log(LoggingLevel.INFO, log, "qdist010 har sendt notifikasjon for forsendelse med " + getIdsForLogging())
				.bean(dokdistStatusUpdater)
				.log(LoggingLevel.INFO, log, "qdist010 har sendt varsel og oppdatert forsendelseStatus=EKSPEDERT og varselStatus=OPPRETTET i dokdistDb for forsendelse med " + getIdsForLogging());
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}, " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}, " +
				"journalpostId=${exchangeProperty." + PROPERTY_JOURNALPOST_ID + "} og " +
				"varselbestillingId=${exchangeProperty." + PROPERTY_VARSELBESTILLING_ID + "}";
	}

	public DataFormat setupVarselFormat() {
		return getJaxbDataFormatForNonRoot(VarselMedHandling.class.getPackage().getName(),
				VarselMedHandling.class,
				"http://nav.no/melding/virksomhet/varselMedHandling/v1/varselMedHandling",
				"varselMedHandling");
	}

	public DataFormat setupDokumentHenvendelseFormat() {
		return getJaxbDataFormatForNonRoot(Dokumenthenvendelse.class.getPackage().getName(),
				Dokumenthenvendelse.class,
				"http://nav.no/melding/virksomhet/opprettDokumenthenvendelse/v1/opprettDokumenthenvendelse",
				"dokumenthenvendelse");
	}

	private static JaxbDataFormat getJaxbDataFormatForNonRoot(String packageName, Class<?> clazz, String namespaceURI, String localPart) {
		JaxbDataFormat result = new JaxbDataFormat(packageName);
		result.setPartClass(clazz);
		result.setFragment(true);
		result.setPartNamespace(new QName(
				namespaceURI,
				localPart));
		result.setEncoding(StandardCharsets.UTF_8.toString());
		return result;
	}
}
