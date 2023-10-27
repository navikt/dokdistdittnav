package no.nav.dokdistdittnav.kdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.dokarkiv.DokarkivConsumer;
import no.nav.dokdistdittnav.consumer.dokarkiv.JournalpostId;
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

import static java.util.Objects.nonNull;
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
	public void updateVarselStatus(Exchange exchange) {
		HoveddokumentLest hoveddokumentLest = exchange.getIn().getBody(HoveddokumentLest.class);
		log.info("Kdist001 mottatt HoveddokumentLest hendelse med journalpostId={}, dokumentInfoId={}, record(topic={}, partition={}, offset={})",
				hoveddokumentLest.getJournalpostId(), hoveddokumentLest.getDokumentInfoId(),
				exchange.getIn().getHeader(TOPIC, String.class), exchange.getIn().getHeader(PARTITION, String.class), exchange.getIn().getHeader(OFFSET, String.class));

		String forsendelseId = administrerForsendelse.finnForsendelse(FinnForsendelseRequest.builder()
				.oppslagsnoekkel(JOURNALPOSTID)
				.verdi(hoveddokumentLest.getJournalpostId())
				.build());

		if (forsendelseId != null) {
			HentForsendelseResponse hentForsendelseResponse = administrerForsendelse.hentForsendelse(forsendelseId);

			if (nonNull(hentForsendelseResponse) && isValidForsendelse(hentForsendelseResponse, hoveddokumentLest)) {

				NokkelInput nokkelInput = mapper.mapNokkelIntern(forsendelseId, dokdistdittnavProperties.getAppnavn(), hentForsendelseResponse);
				kafkaEventProducer.publish(dokdistdittnavProperties.getBrukernotifikasjon().getTopicdone(), nokkelInput, mapper.mapDoneInput());
				dokarkivConsumer.settTidLestHoveddokument(new JournalpostId(hoveddokumentLest.getJournalpostId()), new OppdaterDistribusjonsInfo(OffsetDateTime.now()));
				administrerForsendelse.oppdaterForsendelse(new OppdaterForsendelseRequest(Long.valueOf(forsendelseId), null, FERDIGSTILT));
				log.info("Kdist001 behandlet ferdig HoveddokumentLest hendelse med journalpostId={}, dokumentInfoId={}, record(topic={}, partition={}, offset={})",
						hoveddokumentLest.getJournalpostId(), hoveddokumentLest.getDokumentInfoId(),
						exchange.getIn().getHeader(TOPIC, String.class), exchange.getIn().getHeader(PARTITION, String.class), exchange.getIn().getHeader(OFFSET, String.class));
			}
		}
	}

	private boolean isValidForsendelse(HentForsendelseResponse hentForsendelseResponse, HoveddokumentLest hoveddokumentLest) {
		return isValidStatusAndKanal(hentForsendelseResponse) && isHoveddokument(hentForsendelseResponse, hoveddokumentLest);
	}

	private boolean isValidStatusAndKanal(HentForsendelseResponse hentForsendelseResponse) {
		return OPPRETTET.name().equals(hentForsendelseResponse.getVarselStatus()) && KANAL_DITTNAV.equals(hentForsendelseResponse.getDistribusjonKanal());
	}

	public boolean isHoveddokument(HentForsendelseResponse hentForsendelseResponse, HoveddokumentLest hoveddokumentLest) {
		return nonNull(hentForsendelseResponse.getDokumenter()) && hentForsendelseResponse.getDokumenter().stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.anyMatch(dokument -> dokument.getArkivDokumentInfoId().equals(hoveddokumentLest.getDokumentInfoId())
				);
	}

}
