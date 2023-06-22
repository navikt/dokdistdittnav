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
import no.nav.dokdistdittnav.exception.functional.UtenforKjernetidFunctionalException;
import no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;

import static java.util.Objects.nonNull;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_JOURNALPOST_ID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.mapBeskjedIntern;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.oppretteOppgave;

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
	public void oppretteOppgaveEllerBeskjed(DistribuerTilKanal distribuerTilKanal, Exchange exchange) {
		String forsendelseId = distribuerTilKanal.getForsendelseId();
		HentForsendelseResponse hentForsendelseResponse = administrerForsendelse.hentForsendelse(forsendelseId);
		log.info("Hentet forsendelse med forsendlseId={} og bestillingsId={} fra rdist002", forsendelseId, hentForsendelseResponse.getBestillingsId());
		NokkelInput nokkelIntern = brukerNotifikasjonMapper.mapNokkelIntern(forsendelseId, properties.getAppnavn(), hentForsendelseResponse);

		if (isJournalpostIdNotNull(hentForsendelseResponse)) {
			exchange.setProperty(PROPERTY_JOURNALPOST_ID, hentForsendelseResponse.getArkivInformasjon().getArkivId());
		}
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, hentForsendelseResponse.getBestillingsId());

		if 	(!innenKjernetid(hentForsendelseResponse.getDistribusjonstidspunkt())){
			log.info("Legger melding med distribusjonstidspunkt {} på vente-kø for eventId/bestillingsId={}", hentForsendelseResponse.getDistribusjonstidspunkt(), hentForsendelseResponse.getBestillingsId());
			throw new UtenforKjernetidFunctionalException("Utenfor kjernetid, legges på ventekø");
		} else {
			behandleForsendelse(hentForsendelseResponse, nokkelIntern);
		}
	}

	private void behandleForsendelse(HentForsendelseResponse hentForsendelseResponse, NokkelInput nokkelIntern){
		if (erVedtakEllerViktig(hentForsendelseResponse.getDistribusjonstype()) && isJournalpostIdNotNull(hentForsendelseResponse)) {
			OppgaveInput oppgaveIntern = oppretteOppgave(properties.getBrukernotifikasjon().getLink(), hentForsendelseResponse);
			log.info("Opprettet eventType OPPGAVE med eventId/bestillingsId={}", hentForsendelseResponse.getBestillingsId());
			kafkaEventProducer.publish(properties.getBrukernotifikasjon().getTopicoppgave(), nokkelIntern, oppgaveIntern);
			log.info("Oppgave med eventId/bestillingsId={} skrevet til topic={}", hentForsendelseResponse.getBestillingsId(), properties.getBrukernotifikasjon().getTopicoppgave());
		}

		if (!erVedtakEllerViktig(hentForsendelseResponse.getDistribusjonstype()) && isJournalpostIdNotNull(hentForsendelseResponse)) {
			BeskjedInput beskjedIntern = mapBeskjedIntern(properties.getBrukernotifikasjon().getLink(), hentForsendelseResponse);
			log.info("Opprettet eventType BESKJED med eventId/bestillingsId={}", hentForsendelseResponse.getBestillingsId());
			kafkaEventProducer.publish(properties.getBrukernotifikasjon().getTopicbeskjed(), nokkelIntern, beskjedIntern);
			log.info("Beskjed med eventId/bestillingsId={} skrevet til topic={}", hentForsendelseResponse.getBestillingsId(), properties.getBrukernotifikasjon().getTopicbeskjed());
		}
	}

	private boolean innenKjernetid(DistribusjonstidspunktKode distribusjonstidspunkt) {
		if (distribusjonstidspunkt == null || distribusjonstidspunkt.equals(DistribusjonstidspunktKode.UMIDDELBART)) {
			return true;
		}
		LocalTime tid = LocalTime.now(clock);
		return (tid.isAfter(kjernetidStart) && tid.isBefore(kjernetidSlutt));
	}

	private boolean erVedtakEllerViktig(DistribusjonsTypeKode distribusjonsType) {
		return nonNull(distribusjonsType) && ((VIKTIG.equals(distribusjonsType) || VEDTAK.equals(distribusjonsType)));
	}

	private boolean isJournalpostIdNotNull(HentForsendelseResponse hentForsendelseResponse) {
		return hentForsendelseResponse.getArkivInformasjon() != null && hentForsendelseResponse.getArkivInformasjon().getArkivId() != null;
	}
}
