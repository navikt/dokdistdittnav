package no.nav.dokdistdittnav.kdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.dokarkiv.DokarkivConsumer;
import no.nav.dokdistdittnav.consumer.dokarkiv.JournalPostId;
import no.nav.dokdistdittnav.consumer.dokarkiv.OppdaterDistribusjonsInfo;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.safselvbetjening.schemas.HoveddokumentLest;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdittnav.constants.DomainConstants.HOVEDDOKUMENT;
import static no.nav.dokdistdittnav.constants.DomainConstants.KANAL_DITTNAV;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatus.FERDIGSTILT;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatus.OPPRETTET;

@Slf4j
@Component
public class Ferdigprodusent {

	private static final String JOURNALPOSTID = "journalpostId";

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

		FinnForsendelseResponseTo finnForsendelseResponse = administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(JOURNALPOSTID)
				.verdi(hoveddokumentLest.getJournalpostId())
				.build());

		if (nonNull(finnForsendelseResponse) && nonNull(finnForsendelseResponse.getForsendelseId())) {

			HentForsendelseResponseTo hentForsendelseResponse = administrerForsendelse.hentForsendelse(requireNonNull(finnForsendelseResponse.getForsendelseId(), format("Fant ikke forsendelse med journalpostId=%s", hoveddokumentLest.getJournalpostId())));

			if (nonNull(hentForsendelseResponse) && isValidForsendelse(hentForsendelseResponse, hoveddokumentLest)) {
				log.info("Hentet forsendelse med forsendelseId={} og bestillingsId={} fra dokdist databasen.", finnForsendelseResponse.getForsendelseId(), hentForsendelseResponse.getBestillingsId());

				NokkelInput nokkelInput = mapper.mapNokkelIntern(finnForsendelseResponse.getForsendelseId(), dokdistdittnavProperties.getAppnavn(), hentForsendelseResponse);
				kafkaEventProducer.publish(dokdistdittnavProperties.getBrukernotifikasjon().getTopicdone(), nokkelInput, mapper.mapDoneInput());
				dokarkivConsumer.settTidLestHoveddokument(new JournalPostId(hoveddokumentLest.getJournalpostId()), new OppdaterDistribusjonsInfo(OffsetDateTime.now()));
				administrerForsendelse.oppdaterVarselStatus(finnForsendelseResponse.getForsendelseId(), FERDIGSTILT.name());
				log.info("Oppdatert forsendelse med forsendelseId={} til varselStatus={}", finnForsendelseResponse.getForsendelseId(), FERDIGSTILT);
			}
		}

	}

	private boolean isValidForsendelse(HentForsendelseResponseTo hentForsendelseResponse, HoveddokumentLest hoveddokumentLest) {
		return isValidStatusAndKanal(hentForsendelseResponse) && isHovedDokument(hentForsendelseResponse, hoveddokumentLest);
	}

	private boolean isValidStatusAndKanal(HentForsendelseResponseTo hentForsendelseResponse) {
		return OPPRETTET.name().equals(hentForsendelseResponse.getVarselStatus()) && KANAL_DITTNAV.equals(hentForsendelseResponse.getDistribusjonKanal());
	}

	public boolean isHovedDokument(HentForsendelseResponseTo hentForsendelseResponse, HoveddokumentLest hoveddokumentLest) {
		return nonNull(hentForsendelseResponse.getDokumenter()) && hentForsendelseResponse.getDokumenter().stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.anyMatch(dokument -> dokument.getArkivDokumentInfoId().equals(hoveddokumentLest.getDokumentInfoId())
				);
	}

}
