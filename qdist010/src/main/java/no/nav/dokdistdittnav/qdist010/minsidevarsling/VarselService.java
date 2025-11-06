package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonstidspunktKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.ForsendelseErIkkeGyldigForDistribusjonTilMinSideException;
import no.nav.dokdistdittnav.exception.functional.UtenforKjernetidException;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.time.LocalTime;

import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_JOURNALPOST_ID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonstidspunktKode.UMIDDELBART;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.opprettBeskjed;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.opprettOppgave;

@Slf4j
@Component
public class VarselService {

	private final KafkaEventProducer kafkaEventProducer;
	private final AdministrerForsendelse administrerForsendelse;
	private final DokdistdittnavProperties properties;
	private final LocalTime kjernetidStart;
	private final LocalTime kjernetidSlutt;
	private final Clock clock;

	public VarselService(KafkaEventProducer kafkaEventProducer,
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
		this.clock = clock;
	}

	@Handler
	public void sendVarselTilMinSide(DistribuerTilKanal distribuerTilKanal, Exchange exchange) {
		String forsendelseId = distribuerTilKanal.getForsendelseId();

		HentForsendelseResponse forsendelse = administrerForsendelse.hentForsendelse(forsendelseId);
		log.info("Hentet forsendelse med forsendelseId={} og bestillingsId={} fra rdist001", forsendelseId, forsendelse.getBestillingsId());
		settExchangeProperties(exchange, forsendelse);

		if (forsendelseHarUgyldigStatus(forsendelse)) {
			log.error("Forsendelse med bestillingsId={} har feil forsendelseStatus, eller er ikke arkivert i Joark. Avbryter sending av varsel til Min Side.", forsendelse.getBestillingsId());
			throw new ForsendelseErIkkeGyldigForDistribusjonTilMinSideException("Forsendelse har ugyldig forsendelseStatus, eller er ikke arkivert. Legges på funk. feilkø.");
		}

		if (!innenforKjernetid(forsendelse.getDistribusjonstidspunkt())) {
			log.info("Legger melding med distribusjonstidspunkt={} på vente-kø for varselId (bestillingsId)={}", forsendelse.getDistribusjonstidspunkt(), forsendelse.getBestillingsId());
			throw new UtenforKjernetidException("Utenfor kjernetid, legges på ventekø");
		} else {
			sendOppgaveEllerBeskjed(forsendelse);
		}
	}

	private void sendOppgaveEllerBeskjed(HentForsendelseResponse forsendelse) {
		String lenkeTilVarsel = lagLenkeMedTemaOgArkivId(properties.getMinside().getDokumentarkivLink(), forsendelse);

		String varselId = forsendelse.getBestillingsId();
		DistribusjonsTypeKode distribusjonstype = forsendelse.getDistribusjonstype();

		if (erDistribusjonstypeVedtakViktigEllerNull(distribusjonstype)) {
			String oppgave = opprettOppgave(forsendelse, lenkeTilVarsel);
			log.info("Har opprettet oppgave med varselId (bestillingsId)={}", varselId);

			kafkaEventProducer.publish(properties.getMinside().getVarseltopic(), varselId, oppgave);
			log.info("Har sendt oppgave med varselId (bestillingsId)={} til topic={}", varselId, properties.getMinside().getVarseltopic());
		}

		if (erDistribusjonstypeAnnet(distribusjonstype)) {
			String beskjed = opprettBeskjed(forsendelse, lenkeTilVarsel);
			log.info("Har opprettet beskjed med varselId (bestillingsId)={}", varselId);

			kafkaEventProducer.publish(properties.getMinside().getVarseltopic(), varselId, beskjed);
			log.info("Har sendt beskjed med varselId (bestillingsId)={} til topic={}", varselId, properties.getMinside().getVarseltopic());
		}
	}

	private boolean innenforKjernetid(DistribusjonstidspunktKode distribusjonstidspunkt) {
		if (distribusjonstidspunkt == null || distribusjonstidspunkt.equals(UMIDDELBART)) {
			return true;
		}

		LocalTime tid = LocalTime.now(clock);
		return (tid.isAfter(kjernetidStart) && tid.isBefore(kjernetidSlutt));
	}

	private boolean forsendelseHarUgyldigStatus(HentForsendelseResponse forsendelse) {
		return !erArkivert(forsendelse) || !KLAR_FOR_DIST.name().equals(forsendelse.getForsendelseStatus());
	}

	// For at lenken til dokumentarkivet skal fungere må journalposten ligge i Joark
	private boolean erArkivert(HentForsendelseResponse forsendelse) {
		return forsendelse.getArkivInformasjon() != null && forsendelse.getArkivInformasjon().getArkivId() != null;
	}

	private boolean erDistribusjonstypeVedtakViktigEllerNull(DistribusjonsTypeKode distribusjonstype) {
		return distribusjonstype == null || distribusjonstype == VIKTIG || distribusjonstype == VEDTAK;
	}

	private boolean erDistribusjonstypeAnnet(DistribusjonsTypeKode distribusjonstype) {
		return distribusjonstype == ANNET;
	}

	public static String lagLenkeMedTemaOgArkivId(String url, HentForsendelseResponse forsendelse) {
		URI uri = UriComponentsBuilder
				.fromUriString(url)
				.path(forsendelse.getTema() + "/" + forsendelse.getArkivInformasjon().getArkivId())
				.build().toUri();

		return uri.toString();
	}

	private void settExchangeProperties(Exchange exchange, HentForsendelseResponse forsendelse) {
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, forsendelse.getBestillingsId());

		if (erArkivert(forsendelse)) {
			exchange.setProperty(PROPERTY_JOURNALPOST_ID, forsendelse.getArkivInformasjon().getArkivId());
		}
	}

}