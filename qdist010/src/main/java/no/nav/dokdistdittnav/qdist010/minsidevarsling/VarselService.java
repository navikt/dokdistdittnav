package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.ForsendelseErIkkeGyldigForDistribusjonTilMinSideException;
import no.nav.dokdistdittnav.exception.functional.UtenforKjernetidException;
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
		settExchangeProperties(exchange, forsendelse);

		if (forsendelse.forsendelseHarUgyldigStatus()) {
			log.error("Forsendelse med bestillingsId={} har feil forsendelseStatus, eller er ikke arkivert i Joark. Avbryter sending av varsel til Min Side.", forsendelse.getBestillingsId());
			throw new ForsendelseErIkkeGyldigForDistribusjonTilMinSideException("Forsendelse har ugyldig forsendelseStatus, eller er ikke arkivert. Legges på funk. feilkø.");
		}

		if (forsendelse.skalDistribueresSenere(clock, kjernetidStart, kjernetidSlutt)) {
			log.info("Legger melding med distribusjonstidspunkt={} på vente-kø for varselId (bestillingsId)={}", forsendelse.getDistribusjonstidspunkt(), forsendelse.getBestillingsId());
			throw new UtenforKjernetidException("Utenfor kjernetid, legges på ventekø");
		}

		String oppgaveEllerBeskjed = opprettOppgaveEllerBeskjed(forsendelse);

		sendVarsel(forsendelse.getBestillingsId(), oppgaveEllerBeskjed);
	}

	private String opprettOppgaveEllerBeskjed(HentForsendelseResponse forsendelse) {
		String dokumentarkivLenke = properties.getMinside().getDokumentarkivLink();
		String varselId = forsendelse.getBestillingsId();

		if (forsendelse.erDistribusjonstypeVedtakViktigEllerNull()) {
			String oppgave = opprettOppgave(forsendelse, dokumentarkivLenke);
			log.info("Har opprettet oppgave med varselId (bestillingsId)={}", varselId);
			return oppgave;
		} else {
			String beskjed = opprettBeskjed(forsendelse, dokumentarkivLenke);
			log.info("Har opprettet beskjed med varselId (bestillingsId)={}", varselId);
			return beskjed;
		}
	}

	private void sendVarsel(String varselId, String varsel) {
		kafkaEventProducer.publish(properties.getMinside().getVarseltopic(), varselId, varsel);
		log.info("Har sendt varsel med varselId (bestillingsId)={}", varselId);
	}

	private void settExchangeProperties(Exchange exchange, HentForsendelseResponse forsendelse) {
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, forsendelse.getBestillingsId());

		if (forsendelse.erArkivertIJoark()) {
			exchange.setProperty(PROPERTY_JOURNALPOST_ID, forsendelse.getArkivInformasjon().getArkivId());
		}
	}

}