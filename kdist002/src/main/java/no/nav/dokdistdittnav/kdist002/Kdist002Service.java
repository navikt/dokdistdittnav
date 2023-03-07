package no.nav.dokdistdittnav.kdist002;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.doknotifikasjon.DoknotifikasjonConsumer;
import no.nav.dokdistdittnav.consumer.doknotifikasjon.NotifikasjonInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus;
import no.nav.dokdistdittnav.consumer.rdist001.to.FeilRegistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseResponse;
import no.nav.dokdistdittnav.kafka.DoneEventRequest;
import no.nav.dokdistdittnav.kdist002.mapper.OpprettForsendelseMapper;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.BEKREFTET;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatus.OPPRETTET;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FERDIGSTILT;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.OVERSENDT;
import static no.nav.dokdistdittnav.kdist002.mapper.OppdaterVarselInfoMapper.mapNotifikasjonBestilling;
import static no.nav.dokdistdittnav.utils.DokdistUtils.assertNotBlank;
import static no.nav.dokdistdittnav.utils.DokdistUtils.assertNotNull;
import static org.apache.commons.lang3.StringUtils.substring;

@Slf4j
@Component
public class Kdist002Service {

	private static final String VARSLINGSFEIL = "VARSLINGSFEIL";

	private final DokdistdittnavProperties properties;
	private final AdministrerForsendelse administrerForsendelse;
	private final OpprettForsendelseMapper opprettForsendelseMapper;
	private final DoknotifikasjonConsumer doknotifikasjonConsumer;

	public Kdist002Service(DokdistdittnavProperties properties, AdministrerForsendelse administrerForsendelse, DoknotifikasjonConsumer doknotifikasjonConsumer) {
		this.properties = properties;
		this.administrerForsendelse = administrerForsendelse;
		this.opprettForsendelseMapper = new OpprettForsendelseMapper();
		this.doknotifikasjonConsumer = doknotifikasjonConsumer;
	}

	@Handler
	public DoneEventRequest sendForsendelse(DoknotifikasjonStatus doknotifikasjonStatus) {
		log.info("Kdist002 hentet doknotifikasjonstatus med bestillingsId={} og status={} fra topic={}.", doknotifikasjonStatus.getBestillingsId(), doknotifikasjonStatus.getStatus(), properties.getDoknotifikasjon().getStatustopic());
		String oldBestillingsId = extractDokdistBestillingsId(doknotifikasjonStatus.getBestillingsId());
		FinnForsendelseResponseTo finnForsendelse = finnForsendelse(oldBestillingsId);

		if (skalInformasjonOmVarselLagres(doknotifikasjonStatus)) {

			NotifikasjonInfoTo notifikasjonInfoTo = doknotifikasjonConsumer.getNotifikasjonInfo(doknotifikasjonStatus.getBestillingsId());
			log.info("Kdist002 oppdaterer distribusjonsinfo for notifikasjonen={} for bestillingsId={} med forsendelseID={}", notifikasjonInfoTo.id(), oldBestillingsId, finnForsendelse.getForsendelseId());

			administrerForsendelse.oppdaterVarselInfo(mapNotifikasjonBestilling(finnForsendelse.getForsendelseId(), notifikasjonInfoTo));
			log.info("Kdist002 har oppdatert distribusjonsinfo for notifikasjonen={} for bestillingsId={} med forsendelseID={}", notifikasjonInfoTo.id(), oldBestillingsId, finnForsendelse.getForsendelseId());

			oppdaterForsendelseStatus(finnForsendelse, oldBestillingsId);
		}

		if (!FEILET.name().equals(doknotifikasjonStatus.getStatus())) {
			log.info("Kdist002 bestillingsId={} har ikke status feilet. Avslutter behandlingen", doknotifikasjonStatus.getBestillingsId());
			return null;
		}

		validateFinnForsendelse(finnForsendelse);
		HentForsendelseResponseTo hentForsendelseResponse = administrerForsendelse.hentForsendelse(finnForsendelse.getForsendelseId());
		log.info("Hentet forsendelse med bestillingsId={}, varselStatus={} og forsendelseStatus={} ", hentForsendelseResponse.getBestillingsId(), hentForsendelseResponse.getVarselStatus(), hentForsendelseResponse.getForsendelseStatus());

		return (isOpprettetVarselStatus(hentForsendelseResponse)) ?
				createNewAndFeilRegistrerOldForsendelse(finnForsendelse.getForsendelseId(), hentForsendelseResponse, doknotifikasjonStatus) : null;
	}

	private void oppdaterForsendelseStatus(FinnForsendelseResponseTo finnForsendelse, String bestillingsId) {
		HentForsendelseResponseTo forsendelse = administrerForsendelse.hentForsendelse(finnForsendelse.getForsendelseId());

		if (nonNull(forsendelse)) {
			if (skalOppdatereForsendelseStatus(forsendelse)) {
				log.info("Kdist002 oppdaterer forsendelse med forsendelseId={} til forsendelseStatus=EKSPEDERT for bestillingsid={}", finnForsendelse.getForsendelseId(), bestillingsId);
				administrerForsendelse.oppdaterForsendelseStatus(finnForsendelse.getForsendelseId(), EKSPEDERT.name());
				log.info("Kdist002 har oppdatert forsendelsesstatus med forsendelseId={} til forsendelseStatus=EKSPEDERT for bestillingsid={}", finnForsendelse.getForsendelseId(), bestillingsId);
			} else {
				log.info("Kdist002 skal ikke oppdatere forsendelsestatus på forsendelse med forsendelseId={}, bestillingsId={} og forsendelsestatus={}",
						finnForsendelse.getForsendelseId(), bestillingsId, forsendelse.getForsendelseStatus());
			}
		} else {
			log.info("Kdist002 kan ikke oppdatere forsendelsestatus på forsendelse med forsendelseId={} og bestillingsId={}, siden forsendelse er null",
					finnForsendelse.getForsendelseId(), bestillingsId);
		}
	}

	private static boolean skalOppdatereForsendelseStatus(HentForsendelseResponseTo forsendelse) {
		return ForsendelseStatus.OVERSENDT.name().equals(forsendelse.getForsendelseStatus()) ||
				BEKREFTET.name().equals(forsendelse.getForsendelseStatus());
	}

	private static boolean skalInformasjonOmVarselLagres(DoknotifikasjonStatus doknotifikasjonStatus) {
		return (OVERSENDT.name().equals(doknotifikasjonStatus.getStatus()) ||
				FERDIGSTILT.name().equals(doknotifikasjonStatus.getStatus())
		) && isNull(doknotifikasjonStatus.getDistribusjonId());
	}


	private FinnForsendelseResponseTo finnForsendelse(String bestillingsId) {
		return administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(bestillingsId)
				.build());
	}

	private DoneEventRequest createNewAndFeilRegistrerOldForsendelse(String gammelForsendelseId, HentForsendelseResponseTo hentForsendelseResponse, DoknotifikasjonStatus doknotifikasjonStatus) {
		String gammelBestillingsId = hentForsendelseResponse.getBestillingsId();
		String nyBestillingsId = UUID.randomUUID().toString();
		OpprettForsendelseRequest request = opprettForsendelseMapper.map(hentForsendelseResponse, nyBestillingsId);

		log.info("Kdist002 skal opprette ny forsendelse med bestillingsId={}, og feilregistrere forsendelse={} med bestillingsId={}", nyBestillingsId, gammelForsendelseId, gammelBestillingsId);
		OpprettForsendelseResponse opprettForsendelseResponse = administrerForsendelse.opprettForsendelse(request);
		validateOppdaterForsendelse(opprettForsendelseResponse);

		String nyForsendelseId = opprettForsendelseResponse.getForsendelseId().toString();

		log.info("Kdist002 har opprettet ny forsendelse med forsendelseId={} og bestillingsId={} i dokdist-databasen.", opprettForsendelseResponse.getForsendelseId(), nyBestillingsId);

		feilregistrerForsendelse(gammelForsendelseId, nyBestillingsId, doknotifikasjonStatus);
		log.info("Kdist002 har feilregistrert forsendelse med forsendelseId={} og bestillingsId={} i dokdist-databasen.", gammelForsendelseId, gammelBestillingsId);

		administrerForsendelse.oppdaterForsendelseStatus(nyForsendelseId, KLAR_FOR_DIST.name());

		return DoneEventRequest.builder()
				.dittnavFeiletForsendelseId(gammelForsendelseId)
				.printForsendelseId(nyForsendelseId)
				.dittnavBestillingsId(gammelBestillingsId)
				.printBestillingsId(nyBestillingsId)
				.mottakerId(getMottakerId(hentForsendelseResponse))
				.build();
	}

	private void feilregistrerForsendelse(String gammelForsendelseId, String nyBestillingsId, DoknotifikasjonStatus doknotifikasjonStatus) {

		administrerForsendelse.feilregistrerForsendelse(FeilRegistrerForsendelseRequest.builder()
				.forsendelseId(gammelForsendelseId)
				.type(VARSLINGSFEIL)
				.tidspunkt(LocalDateTime.now())
				.detaljer(doknotifikasjonStatus.getMelding())
				.resendingDistribusjonId(nyBestillingsId)
				.build());
	}

	private String getMottakerId(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return Optional.ofNullable(hentForsendelseResponseTo.getMottaker())
				.map(HentForsendelseResponseTo.MottakerTo::getMottakerId).orElseThrow(() -> new IllegalArgumentException("MottakerId kan ikke være null"));
	}

	private String extractDokdistBestillingsId(String doknotifikasjonBestillingsId) {
		return substring(doknotifikasjonBestillingsId, doknotifikasjonBestillingsId.length() - 36);
	}

	private boolean isOpprettetVarselStatus(HentForsendelseResponseTo hentForsendelseResponse) {
		return OPPRETTET.name().equals(hentForsendelseResponse.getVarselStatus()) &&
				!ForsendelseStatus.FEILET.name().equals(hentForsendelseResponse.getForsendelseStatus());
	}

	private void validateOppdaterForsendelse(OpprettForsendelseResponse request) {
		assertNotNull("OpprettForsendelseResponse", request);
		assertNotBlank("OpprettForsendelseResponse.ForsendelseId", valueOf(request.getForsendelseId()));
	}

	private void validateFinnForsendelse(FinnForsendelseResponseTo finnForsendelseResponseTo) {
		assertNotNull("finnForsendelseResponseTo", finnForsendelseResponseTo);
		assertNotBlank("FinnForsendelseResponseTo.ForsendelseId", valueOf(finnForsendelseResponseTo.getForsendelseId()));
	}
}
