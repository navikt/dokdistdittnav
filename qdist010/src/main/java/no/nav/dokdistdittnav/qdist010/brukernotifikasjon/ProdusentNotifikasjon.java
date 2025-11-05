package no.nav.dokdistdittnav.qdist010.brukernotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonstidspunktKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.UtenforKjernetidException;
import no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;

import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_JOURNALPOST_ID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonstidspunktKode.UMIDDELBART;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.opprettBeskjed;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.opprettOppgave;

@Slf4j
@Component
public class ProdusentNotifikasjon {

	private final KafkaEventProducer kafkaEventProducer;
	private final AdministrerForsendelse administrerForsendelse;
	private final BrukerNotifikasjonMapper brukerNotifikasjonMapper;
	private final DokdistdittnavProperties properties;
	private final LocalTime kjernetidStart;
	private final LocalTime kjernetidSlutt;
	private final Clock clock;

	public ProdusentNotifikasjon(KafkaEventProducer kafkaEventProducer,
								 AdministrerForsendelse administrerForsendelse,
								 DokdistdittnavProperties properties,
								 @Value("${kjernetidStart}") String kjernetidStart,
								 @Value("${kjernetidSlutt}") String kjernetidSlutt,
								 Clock clock) {
		this.kafkaEventProducer = kafkaEventProducer;
		this.administrerForsendelse = administrerForsendelse;
		this.properties = properties;
		this.kjernetidStart = LocalTime.parse(kjernetidStart);
		this.kjernetidSlutt = LocalTime.parse(kjernetidSlutt);
		this.brukerNotifikasjonMapper = new BrukerNotifikasjonMapper();
		this.clock = clock;
	}

	@Handler
	public void opprettOppgaveEllerBeskjed(DistribuerTilKanal distribuerTilKanal, Exchange exchange) {
		String forsendelseId = distribuerTilKanal.getForsendelseId();
		HentForsendelseResponse forsendelse = administrerForsendelse.hentForsendelse(forsendelseId);
		log.info("Hentet forsendelse med forsendelseId={} og bestillingsId={} fra rdist001", forsendelseId, forsendelse.getBestillingsId());

		settExchangeProperties(exchange, forsendelse);

		if (!innenforKjernetid(forsendelse.getDistribusjonstidspunkt())) {
			log.info("Legger melding med distribusjonstidspunkt={} på vente-kø for eventId/bestillingsId={}", forsendelse.getDistribusjonstidspunkt(), forsendelse.getBestillingsId());
			throw new UtenforKjernetidException("Utenfor kjernetid, legges på ventekø");
		} else {
			publiserOppgaveEllerBeskjed(forsendelse, brukerNotifikasjonMapper.mapNokkelIntern(forsendelseId, properties.getAppnavn(), forsendelse));
		}
	}

	private void publiserOppgaveEllerBeskjed(HentForsendelseResponse forsendelse, NokkelInput noekkel) {
		var distribusjonstype = forsendelse.getDistribusjonstype();

		if (erDistribusjonstypeVedtakViktigEllerNull(distribusjonstype) && harJournalpostId(forsendelse)) {
			String oppgave = opprettOppgave(properties.getBrukernotifikasjon().getLink(), forsendelse);
			log.info("Har opprettet oppgave med varselId (bestillingsId)={}", forsendelse.getBestillingsId());

			// Bruk samme topic
			kafkaEventProducer.publish(properties.getBrukernotifikasjon().getTopicoppgave(), noekkel, oppgave);
			log.info("Har publisert oppgave med varselId (bestillingsId)={} til topic={}", forsendelse.getBestillingsId(), properties.getBrukernotifikasjon().getTopicoppgave());
		}

		if (erDistribusjonstypeAnnet(distribusjonstype) && harJournalpostId(forsendelse)) {
			String beskjed = opprettBeskjed(properties.getBrukernotifikasjon().getLink(), forsendelse);
			log.info("Har opprettet beskjed med varselId (bestillingsId)={}", forsendelse.getBestillingsId());

			// Bruk samme topic
			kafkaEventProducer.publish(properties.getBrukernotifikasjon().getTopicbeskjed(), noekkel, beskjed);
			log.info("Har publisert beskjed med varselId (bestillingsId)={} til topic={}", forsendelse.getBestillingsId(), properties.getBrukernotifikasjon().getTopicbeskjed());
		}
	}

	private boolean innenforKjernetid(DistribusjonstidspunktKode distribusjonstidspunkt) {
		if (distribusjonstidspunkt == null || distribusjonstidspunkt.equals(UMIDDELBART)) {
			return true;
		}

		LocalTime tid = LocalTime.now(clock);
		return (tid.isAfter(kjernetidStart) && tid.isBefore(kjernetidSlutt));
	}

	private boolean erDistribusjonstypeVedtakViktigEllerNull(DistribusjonsTypeKode distribusjonstype) {
		return distribusjonstype == null || distribusjonstype == VIKTIG || distribusjonstype == VEDTAK;
	}

	private boolean erDistribusjonstypeAnnet(DistribusjonsTypeKode distribusjonstype) {
		return distribusjonstype == ANNET;
	}

	private boolean harJournalpostId(HentForsendelseResponse forsendelse) {
		return forsendelse.getArkivInformasjon() != null && forsendelse.getArkivInformasjon().getArkivId() != null;
	}

	private void settExchangeProperties(Exchange exchange, HentForsendelseResponse forsendelse) {
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, forsendelse.getBestillingsId());

		if (harJournalpostId(forsendelse)) {
			exchange.setProperty(PROPERTY_JOURNALPOST_ID, forsendelse.getArkivInformasjon().getArkivId());
		}
	}
}
