package no.nav.dokdistdittnav.kdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.dokarkiv.DokarkivConsumer;
import no.nav.dokdistdittnav.consumer.dokarkiv.OppdaterDistribusjonsInfo;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.OppdaterForsendelseRequest;
import no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.safselvbetjening.schemas.HoveddokumentLest;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

import static no.nav.dokdistdittnav.constants.DomainConstants.HOVEDDOKUMENT;
import static no.nav.dokdistdittnav.constants.DomainConstants.KANAL_DITTNAV;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.Oppslagsnoekkel.JOURNALPOSTID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatusCode.FERDIGSTILT;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatusCode.OPPRETTET;
import static org.apache.camel.component.kafka.KafkaConstants.OFFSET;
import static org.apache.camel.component.kafka.KafkaConstants.PARTITION;
import static org.apache.camel.component.kafka.KafkaConstants.TOPIC;

@Slf4j
@Component
public class BehandleHoveddokumentLestService {

	private final AdministrerForsendelse administrerForsendelse;
	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final DokarkivConsumer dokarkivConsumer;
	private final KafkaEventProducer kafkaEventProducer;
	private final BrukerNotifikasjonMapper mapper;

	public BehandleHoveddokumentLestService(AdministrerForsendelse administrerForsendelse,
											DokdistdittnavProperties dokdistdittnavProperties,
											KafkaEventProducer kafkaEventProducer,
											DokarkivConsumer dokarkivConsumer) {
		this.administrerForsendelse = administrerForsendelse;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
		this.kafkaEventProducer = kafkaEventProducer;
		this.mapper = new BrukerNotifikasjonMapper();
		this.dokarkivConsumer = dokarkivConsumer;
	}

	@Handler
	public void behandleHoveddokumentLest(Exchange exchange) {
		HoveddokumentLest hoveddokumentLest = exchange.getIn().getBody(HoveddokumentLest.class);
		log.info("kdist001 har mottatt HoveddokumentLest-hendelse med journalpostId={}, dokumentInfoId={}, record(topic={}, partition={}, offset={})",
				hoveddokumentLest.getJournalpostId(), hoveddokumentLest.getDokumentInfoId(),
				exchange.getIn().getHeader(TOPIC, String.class), exchange.getIn().getHeader(PARTITION, String.class), exchange.getIn().getHeader(OFFSET, String.class));

		String forsendelseId = finnForsendelse(hoveddokumentLest.getJournalpostId());
		if (forsendelseId == null) {
			return;
		}

		HentForsendelseResponse forsendelse = administrerForsendelse.hentForsendelse(forsendelseId);

		if (isValidForsendelse(forsendelse, hoveddokumentLest)) {
			sendDoneEventTilDittNav(forsendelseId, forsendelse);

			dokarkivConsumer.settDatoLest(hoveddokumentLest.getJournalpostId(), new OppdaterDistribusjonsInfo(OffsetDateTime.now()));
			oppdaterStatusPaaForsendelseTilFerdigstilt(forsendelseId);

			log.info("kdist001 har behandlet ferdig HoveddokumentLest-hendelse med journalpostId={}, dokumentInfoId={}", hoveddokumentLest.getJournalpostId(), hoveddokumentLest.getDokumentInfoId());
		}
	}

	private String finnForsendelse(String journalpostId) {
		return administrerForsendelse.finnForsendelse(FinnForsendelseRequest.builder()
				.oppslagsnoekkel(JOURNALPOSTID)
				.verdi(journalpostId)
				.build());
	}

	private void sendDoneEventTilDittNav(String forsendelseId, HentForsendelseResponse forsendelse) {
		NokkelInput nokkelInput = mapper.mapNokkelIntern(forsendelseId, dokdistdittnavProperties.getAppnavn(), forsendelse);
		kafkaEventProducer.publish(dokdistdittnavProperties.getBrukernotifikasjon().getTopicdone(), nokkelInput, mapper.mapDoneInput());
	}

	private void oppdaterStatusPaaForsendelseTilFerdigstilt(String forsendelseId) {
		administrerForsendelse.oppdaterForsendelse(new OppdaterForsendelseRequest(Long.valueOf(forsendelseId), null, FERDIGSTILT));
	}

	private boolean isValidForsendelse(HentForsendelseResponse forsendelse, HoveddokumentLest hoveddokumentLest) {
		return forsendelse != null && erVarselstatusOpprettetOgKanalDittNav(forsendelse) && erHoveddokumentMedRiktigDokumentInfoId(forsendelse, hoveddokumentLest);
	}

	private boolean erVarselstatusOpprettetOgKanalDittNav(HentForsendelseResponse forsendelse) {
		return OPPRETTET.name().equals(forsendelse.getVarselStatus()) && KANAL_DITTNAV.equals(forsendelse.getDistribusjonKanal());
	}

	public boolean erHoveddokumentMedRiktigDokumentInfoId(HentForsendelseResponse forsendelse, HoveddokumentLest hoveddokumentLest) {
		if (forsendelse.getDokumenter() == null) {
			return false;
		}

		return forsendelse.getDokumenter().stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.anyMatch(dokument -> dokument.getArkivDokumentInfoId().equals(hoveddokumentLest.getDokumentInfoId()));
	}

}
