package no.nav.dokdistdittnav.qdist010.brukernotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern;
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern;
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern;
import no.nav.dokdistdittnav.config.alias.BrukernotifikasjonTopic;
import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;
import static no.nav.dokdistdittnav.consumer.rdist001.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.DistribusjonsTypeKode.VIKTIG;

@Slf4j
@Component
public class ProdusentNotifikasjon {

	private final KafkaEventProducer kafkaEventProducer;
	private final AdministrerForsendelse administrerForsendelse;
	private final ServiceuserAlias serviceuser;
	private final BrukerNotifikasjonMapper brukerNotifikasjonMapper;
	private final BrukernotifikasjonTopic brukernotifikasjonTopic;

	@Autowired
	public ProdusentNotifikasjon(KafkaEventProducer kafkaEventProducer, AdministrerForsendelse administrerForsendelse,
								 ServiceuserAlias serviceuser, BrukernotifikasjonTopic brukernotifikasjonTopic) {
		this.kafkaEventProducer = kafkaEventProducer;
		this.administrerForsendelse = administrerForsendelse;
		this.serviceuser = serviceuser;
		this.brukerNotifikasjonMapper = new BrukerNotifikasjonMapper();
		this.brukernotifikasjonTopic = brukernotifikasjonTopic;
	}

	@Handler
	public void oppretteOppgaveEllerBeskjed(DistribuerTilKanal distribuerTilKanal) {
		String forsendelseId = distribuerTilKanal.getForsendelseId();
		HentForsendelseResponseTo hentForsendelseResponse = administrerForsendelse.hentForsendelse(forsendelseId);
		NokkelIntern nokkelIntern = brukerNotifikasjonMapper.mapNokkelIntern(forsendelseId, serviceuser.getUsername(), hentForsendelseResponse);

		if (erVedtakEllerViktig(hentForsendelseResponse.getDistribusjonstype())) {
			OppgaveIntern oppgaveIntern = brukerNotifikasjonMapper.oppretteOppgave(brukernotifikasjonTopic.getUrl(), hentForsendelseResponse);
			log.info("Oppretter varseling oppgave med eventId/forsendelseId={}", forsendelseId);
			kafkaEventProducer.publish(brukernotifikasjonTopic.getTopicoppgave(), nokkelIntern, oppgaveIntern);
			log.info("Oppgave opprettet fra system={} med eventId/forsendelseId={}.", serviceuser.getUsername(), forsendelseId);
		}

		if (!erVedtakEllerViktig(hentForsendelseResponse.getDistribusjonstype())) {
			BeskjedIntern beskjedIntern = brukerNotifikasjonMapper.mapBeskjedIntern(brukernotifikasjonTopic.getUrl(), hentForsendelseResponse);
			kafkaEventProducer.publish(brukernotifikasjonTopic.getTopicbeskjed(), nokkelIntern, beskjedIntern);
			log.info("Beskjed sendt fra system={} med eventId/forsendelseId={} til Brukernotifikasjon", serviceuser.getUsername(), forsendelseId);
		}
	}

	private boolean erVedtakEllerViktig(DistribusjonsTypeKode distribusjonsType) {
		return nonNull(distribusjonsType) && ((VIKTIG.equals(distribusjonsType) || VEDTAK.equals(distribusjonsType)));
	}

}
