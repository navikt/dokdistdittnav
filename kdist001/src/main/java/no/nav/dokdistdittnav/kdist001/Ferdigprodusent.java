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
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

import static java.util.Objects.nonNull;
import static no.nav.dokdistdittnav.constants.DomainConstants.HOVEDDOKUMENT;
import static no.nav.dokdistdittnav.constants.DomainConstants.KANAL_DITTNAV;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.Oppslagsnoekkel.JOURNALPOSTID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatusCode.FERDIGSTILT;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatusCode.OPPRETTET;

@Slf4j
@Component
public class Ferdigprodusent {

	private final AdministrerForsendelse administrerForsendelse;
	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final DokarkivConsumer dokarkivConsumer;
	private final KafkaEventProducer kafkaEventProducer;
	private final BrukerNotifikasjonMapper mapper;

	@Autowired
	public Ferdigprodusent(AdministrerForsendelse administrerForsendelse, DokdistdittnavProperties dokdistdittnavProperties,
						   KafkaEventProducer kafkaEventProducer, DokarkivConsumer dokarkivConsumer) {
		this.administrerForsendelse = administrerForsendelse;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
		this.kafkaEventProducer = kafkaEventProducer;
		this.mapper = new BrukerNotifikasjonMapper();
		this.dokarkivConsumer = dokarkivConsumer;
	}

	@Handler
	public void updateVarselStatus(HoveddokumentLest hoveddokumentLest) {
		log.info("Mottatt hoveddokumentLest med journalpostId={}, dokumentInfoId={}.", hoveddokumentLest.getJournalpostId(), hoveddokumentLest.getDokumentInfoId());

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
			}
		}
	}

	private boolean isValidForsendelse(HentForsendelseResponse hentForsendelseResponse, HoveddokumentLest hoveddokumentLest) {
		return isValidStatusAndKanal(hentForsendelseResponse) && isHovedDokument(hentForsendelseResponse, hoveddokumentLest);
	}

	private boolean isValidStatusAndKanal(HentForsendelseResponse hentForsendelseResponse) {
		return OPPRETTET.name().equals(hentForsendelseResponse.getVarselStatus()) && KANAL_DITTNAV.equals(hentForsendelseResponse.getDistribusjonKanal());
	}

	public boolean isHovedDokument(HentForsendelseResponse hentForsendelseResponse, HoveddokumentLest hoveddokumentLest) {
		return nonNull(hentForsendelseResponse.getDokumenter()) && hentForsendelseResponse.getDokumenter().stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.anyMatch(dokument -> dokument.getArkivDokumentInfoId().equals(hoveddokumentLest.getDokumentInfoId())
				);
	}

}
