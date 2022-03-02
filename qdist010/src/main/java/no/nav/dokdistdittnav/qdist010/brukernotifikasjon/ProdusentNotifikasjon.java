package no.nav.dokdistdittnav.qdist010.brukernotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.dokdistdittnav.config.alias.DokdistdittnavProperties;
import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;
import static no.nav.dokdistdittnav.constants.DomainConstants.BESTILLING_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.JOURNALPOST_ID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;

@Slf4j
@Component
public class ProdusentNotifikasjon {

	private final KafkaEventProducer kafkaEventProducer;
	private final AdministrerForsendelse administrerForsendelse;
	private final ServiceuserAlias serviceuser;
	private final BrukerNotifikasjonMapper brukerNotifikasjonMapper;
	private final DokdistdittnavProperties properties;

	@Autowired
	public ProdusentNotifikasjon(KafkaEventProducer kafkaEventProducer, AdministrerForsendelse administrerForsendelse,
								 ServiceuserAlias serviceuser, DokdistdittnavProperties properties) {
		this.kafkaEventProducer = kafkaEventProducer;
		this.administrerForsendelse = administrerForsendelse;
		this.serviceuser = serviceuser;
		this.brukerNotifikasjonMapper = new BrukerNotifikasjonMapper();
		this.properties = properties;
	}

	@Handler
	public void oppretteOppgaveEllerBeskjed(DistribuerTilKanal distribuerTilKanal, Exchange exchange) {
		String forsendelseId = distribuerTilKanal.getForsendelseId();
		HentForsendelseResponseTo hentForsendelseResponse = administrerForsendelse.hentForsendelse(forsendelseId);
		NokkelInput nokkelIntern = brukerNotifikasjonMapper.mapNokkelIntern(forsendelseId, properties.getAppnavn(), hentForsendelseResponse);

		exchange.setProperty(JOURNALPOST_ID, hentForsendelseResponse.getArkivInformasjon().getArkivId());
		exchange.setProperty(BESTILLING_ID, hentForsendelseResponse.getBestillingsId());

		if (erVedtakEllerViktig(hentForsendelseResponse.getDistribusjonstype())) {
			OppgaveInput oppgaveIntern = brukerNotifikasjonMapper.oppretteOppgave(properties.getBrukernotifikasjon().getLink(), hentForsendelseResponse);
			log.info("Oppretter varseling oppgave med eventId/forsendelseId={}", forsendelseId);
			kafkaEventProducer.publish(properties.getBrukernotifikasjon().getTopicoppgave(), nokkelIntern, oppgaveIntern);
			log.info("Oppgave opprettet fra system={} med eventId/forsendelseId={}.", serviceuser.getUsername(), forsendelseId);
		}

		if (!erVedtakEllerViktig(hentForsendelseResponse.getDistribusjonstype())) {
			BeskjedInput beskjedIntern = brukerNotifikasjonMapper.mapBeskjedIntern(properties.getBrukernotifikasjon().getLink(), hentForsendelseResponse);
			kafkaEventProducer.publish(properties.getBrukernotifikasjon().getTopicbeskjed(), nokkelIntern, beskjedIntern);
			log.info("Beskjed sendt fra system={} med eventId/forsendelseId={} til Brukernotifikasjon", serviceuser.getUsername(), forsendelseId);
		}
	}

	private boolean erVedtakEllerViktig(DistribusjonsTypeKode distribusjonsType) {
		return nonNull(distribusjonsType) && ((VIKTIG.equals(distribusjonsType) || VEDTAK.equals(distribusjonsType)));
	}

}
