package no.nav.dokdistdittnav.kdist002;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus;
import no.nav.dokdistdittnav.consumer.rdist001.to.FeilRegistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseResponseTo;
import no.nav.dokdistdittnav.kafka.DoneEventRequest;
import no.nav.dokdistdittnav.kdist002.mapper.PersisterForsendelseMapper;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.valueOf;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatus.OPPRETTET;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static no.nav.dokdistdittnav.utils.DokdistUtils.assertNotBlank;
import static no.nav.dokdistdittnav.utils.DokdistUtils.assertNotNull;
import static org.apache.commons.lang3.StringUtils.substring;

@Slf4j
@Component
public class Kdist002Service {

	private static final String VARSLINGSFEIL = "VARSLINGSFEIL";

	private final DokdistdittnavProperties properties;
	private final AdministrerForsendelse administrerForsendelse;
	private final PersisterForsendelseMapper persisterForsendelseMapper;

	public Kdist002Service(DokdistdittnavProperties properties, AdministrerForsendelse administrerForsendelse) {
		this.properties = properties;
		this.administrerForsendelse = administrerForsendelse;
		this.persisterForsendelseMapper = new PersisterForsendelseMapper();
	}

	@Handler
	public DoneEventRequest sendForsendelse(DoknotifikasjonStatus doknotifikasjonStatus) {
		log.info("Hentet doknotifikasjonstatus med bestillingsId={} og status={} fra topic={}.", doknotifikasjonStatus.getBestillingsId(), doknotifikasjonStatus.getStatus(), properties.getDoknotifikasjon().getStatustopic());
		if (!isDittnavAndFeilStatus(doknotifikasjonStatus)) {
			return null;
		} else {
			String oldBestillingsId = extractDokdistBestillingsId(doknotifikasjonStatus.getBestillingsId());
			FinnForsendelseResponseTo finnForsendelse = finnForsendelse(oldBestillingsId);
			validateFinnForsendelse(finnForsendelse);
			HentForsendelseResponseTo hentForsendelseResponse = administrerForsendelse.hentForsendelse(finnForsendelse.getForsendelseId());
			return (isOpprettetVarselStatus(hentForsendelseResponse)) ?
					createNewAndFeilRegistrerOldForsendelse(finnForsendelse.getForsendelseId(), hentForsendelseResponse, doknotifikasjonStatus) : null;
		}
	}

	private FinnForsendelseResponseTo finnForsendelse(String bestillingsId) {
		return administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(bestillingsId)
				.build());
	}

	private DoneEventRequest createNewAndFeilRegistrerOldForsendelse(String oldForsendelseId, HentForsendelseResponseTo hentForsendelseResponse, DoknotifikasjonStatus status) {
		String newBestillingsId = UUID.randomUUID().toString();
		PersisterForsendelseRequestTo request = persisterForsendelseMapper.map(hentForsendelseResponse);
		PersisterForsendelseResponseTo persisterForsendelseResponse = administrerForsendelse.persisterForsendelse(request);
		validateOppdaterForsendelse(persisterForsendelseResponse);
		log.info("Opprettet ny forsendelse med forsendelseId={} i dokdist databasen.", persisterForsendelseResponse.getForsendelseId());
		createFeilRegistrerForsendelse(oldForsendelseId, request, status);
		log.info("Forsendelsen med forsendelseId={} er feilregistrert i dokdist databasen.", oldForsendelseId);
		administrerForsendelse.oppdaterForsendelseStatus(valueOf(persisterForsendelseResponse.getForsendelseId()), KLAR_FOR_DIST.name());
		return DoneEventRequest.builder()
				.forsendelseId(valueOf(persisterForsendelseResponse.getForsendelseId()))
				.bestillingsId(newBestillingsId)
				.mottakerId(getMottakerId(hentForsendelseResponse))
				.build();
	}

	private void createFeilRegistrerForsendelse(String forsendelseId, PersisterForsendelseRequestTo request, DoknotifikasjonStatus status) {

		administrerForsendelse.feilregistrerForsendelse(FeilRegistrerForsendelseRequest.builder()
				.forsendelseId(forsendelseId)
				.type(VARSLINGSFEIL)
				.tidspunkt(LocalDateTime.now())
				.detaljer(status.getMelding())
				.resendingDistribusjonId(request.getBestillingsId())
				.build());
	}

	private String getMottakerId(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return Optional.ofNullable(hentForsendelseResponseTo.getMottaker())
				.map(HentForsendelseResponseTo.MottakerTo::getMottakerId).orElseThrow(() -> new IllegalArgumentException("MottakerId kan ikke være null"));
	}

	private String extractDokdistBestillingsId(String doknotifikasjonBestillingsId) {
		return substring(doknotifikasjonBestillingsId, doknotifikasjonBestillingsId.length() - 36);
	}

	private boolean isDittnavAndFeilStatus(DoknotifikasjonStatus doknotifikasjonStatus) {
		return properties.getAppnavn().equals(doknotifikasjonStatus.getBestillerId()) || FEILET.name().equals(doknotifikasjonStatus.getStatus());
	}

	private boolean isOpprettetVarselStatus(HentForsendelseResponseTo hentForsendelseResponse) {
		return OPPRETTET.name().equals(hentForsendelseResponse.getVarselStatus()) &&
				!ForsendelseStatus.FEILET.name().equals(hentForsendelseResponse.getForsendelseStatus());
	}

	private void validateOppdaterForsendelse(PersisterForsendelseResponseTo request) {
		assertNotNull("PersisterForsendelseResponseTo", request);
		assertNotBlank("PersisterForsendelseResponseTo.ForsendelseId", valueOf(request.getForsendelseId()));
	}

	private void validateFinnForsendelse(FinnForsendelseResponseTo finnForsendelseResponseTo) {
		assertNotNull("finnForsendelseResponseTo", finnForsendelseResponseTo);
		assertNotBlank("FinnForsendelseResponseTo.ForsendelseId", valueOf(finnForsendelseResponseTo.getForsendelseId()));
	}
}
