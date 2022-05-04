package no.nav.dokdistdittnav.qdist010;

import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.exception.functional.UtenforKjernetidFunctionalException;
import no.nav.dokdistdittnav.metrics.Qdist010MetricsRoutePolicy;
import no.nav.dokdistdittnav.qdist010.brukernotifikasjon.ProdusentNotifikasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_JOURNALPOST_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.SERVICE_ID;
import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class Qdist010Route extends RouteBuilder {

	private final ProdusentNotifikasjon produsentNotifikasjon;
	private final DokdistStatusUpdater dokdistStatusUpdater;
	private final Queue qdist010;
	private final Queue qdist010FunksjonellFeil;
	private final Queue qdist010UtenforKjernetid;
	private final Qdist010MetricsRoutePolicy qdist010MetricsRoutePolicy;

	@Autowired
	public Qdist010Route(ProdusentNotifikasjon produsentNotifikasjon,
						 DokdistStatusUpdater dokdistStatusUpdater,
						 Queue qdist010,
						 Queue qdist010FunksjonellFeil,
						 Queue qdist010UtenforKjernetid,
						 Qdist010MetricsRoutePolicy qdist010MetricsRoutePolicy) {
		this.produsentNotifikasjon = produsentNotifikasjon;
		this.dokdistStatusUpdater = dokdistStatusUpdater;
		this.qdist010 = qdist010;
		this.qdist010FunksjonellFeil = qdist010FunksjonellFeil;
		this.qdist010UtenforKjernetid = qdist010UtenforKjernetid;
		this.qdist010MetricsRoutePolicy = qdist010MetricsRoutePolicy;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(true)
				.loggingLevel(ERROR));

		onException(UtenforKjernetidFunctionalException.class)
				.handled(true)
				.useOriginalMessage()
				.log(LoggingLevel.INFO, log, "Forsendelse til DittNAV er utenfor kjernetid. Legges på kø. " + getIdsForLogging())
				.to("jms:" + qdist010UtenforKjernetid.getQueueName());

		onException(AbstractDokdistdittnavFunctionalException.class, JAXBException.class, ValidationException.class)
				.handled(true)
				.useOriginalMessage()
				.log(LoggingLevel.WARN, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist010FunksjonellFeil.getQueueName());

		from("jms:" + qdist010.getQueueName() +
				"?transacted=true")
				.routeId(SERVICE_ID)
				.setExchangePattern(InOnly)
				.routePolicy(qdist010MetricsRoutePolicy)
				.process(new IdsProcessor())
				.log(INFO, log, "qdist010 har mottatt forsendelse med forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}")
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/out/distribuertilkanal.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.process(exchange -> {
					DistribuerTilKanal distribuerTilKanal = exchange.getIn().getBody(DistribuerTilKanal.class);
					exchange.setProperty(PROPERTY_FORSENDELSE_ID, distribuerTilKanal.getForsendelseId());
				})
				.bean(produsentNotifikasjon)
				.log(INFO, log, "qdist010 har sendt notifikasjon for forsendelse med " + getIdsForLogging())
				.bean(dokdistStatusUpdater)
				.log(INFO, log, "qdist010 har sendt varsel og oppdatert forsendelseStatus=EKSPEDERT og varselStatus=OPPRETTET i dokdistDb for forsendelse med " + getIdsForLogging());
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}, " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}, " +
				"journalpostId=${exchangeProperty." + PROPERTY_JOURNALPOST_ID + "}.";
	}
}
