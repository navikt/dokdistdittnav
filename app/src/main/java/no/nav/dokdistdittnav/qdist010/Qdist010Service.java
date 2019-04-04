package no.nav.dokdistdittnav.qdist010;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.qdist010.util.Qdist009FunctionalUtils.getDokumenttypeIdHoveddokument;
import static no.nav.dokdistdittnav.qdist010.util.Qdist009FunctionalUtils.validateForsendelseStatus;

import com.amazonaws.SdkClientException;
import no.nav.dokdistdittnav.metrics.MetricUpdater;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.HentPostDestinasjonResponseTo;
import no.nav.dokdistdittnav.consumer.regoppslag.Regoppslag;
import no.nav.dokdistdittnav.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistdittnav.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistdittnav.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistdittnav.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistdittnav.exception.functional.DokumentIkkeFunnetIS3Exception;
import no.nav.dokdistdittnav.exception.functional.KunneIkkeDeserialisereS3JsonPayloadFunctionalException;
import no.nav.dokdistdittnav.qdist010.domain.Adresse;
import no.nav.dokdistdittnav.qdist010.domain.DistribuerForsendelseTilSentralPrintTo;
import no.nav.dokdistdittnav.storage.DokdistDokument;
import no.nav.dokdistdittnav.storage.JsonSerializer;
import no.nav.dokdistdittnav.storage.Storage;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist010Service {

	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final AdministrerForsendelse administrerForsendelse;
	private final Regoppslag regoppslag;
	private final Storage storage;
	private final MetricUpdater metricUpdater;

	@Inject
	public Qdist010Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   AdministrerForsendelse administrerForsendelse,
						   Storage storage,
						   Regoppslag regoppslag,
						   MetricUpdater metricUpdater) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.administrerForsendelse = administrerForsendelse;
		this.regoppslag = regoppslag;
		this.storage = storage;
		this.metricUpdater = metricUpdater;
	}

	@Handler
	public void distribuerForsendelseTilSentralPrintService(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo) {
		HentForsendelseResponseTo hentForsendelseResponseTo = administrerForsendelse.hentForsendelse(distribuerForsendelseTilSentralPrintTo
				.getForsendelseId());
		validateForsendelseStatus(hentForsendelseResponseTo.getForsendelseStatus());
		DokumenttypeInfoTo dokumenttypeInfoTo = dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(hentForsendelseResponseTo));
		Adresse adresse = getAdresse(hentForsendelseResponseTo);
		HentPostDestinasjonResponseTo hentPostDestinasjonResponseTo = administrerForsendelse.hentPostDestinasjon(adresse.getLandkode());

		List<DokdistDokument> dokdistDokumentList = getDocumentsFromS3(hentForsendelseResponseTo);

		metricUpdater.updateQdist009Metrics(hentPostDestinasjonResponseTo.getPostDestinasjon(), adresse.getLandkode());
	}

	private Adresse getAdresse(HentForsendelseResponseTo hentForsendelseResponseTo) {
		final HentForsendelseResponseTo.PostadresseTo adresseDokdist = hentForsendelseResponseTo.getPostadresse();
		if (adresseDokdist == null) {
			final AdresseTo adresseRegoppslag = getAdresseFromRegoppslag(hentForsendelseResponseTo);
			return Adresse.builder()
					.adresselinje1(adresseRegoppslag.getAdresselinje1())
					.adresselinje2(adresseRegoppslag.getAdresselinje2())
					.adresselinje3(adresseRegoppslag.getAdresselinje3())
					.landkode(adresseRegoppslag.getLandkode())
					.postnummer(adresseRegoppslag.getPostnummer())
					.poststed(adresseRegoppslag.getPoststed())
					.build();
		} else {
			return Adresse.builder()
					.adresselinje1(adresseDokdist.getAdresselinje1())
					.adresselinje2(adresseDokdist.getAdresselinje2())
					.adresselinje3(adresseDokdist.getAdresselinje3())
					.landkode(adresseDokdist.getLandkode())
					.postnummer(adresseDokdist.getPostnummer())
					.poststed(adresseDokdist.getPoststed())
					.build();
		}
	}

	private AdresseTo getAdresseFromRegoppslag(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return regoppslag.treg002HentAdresse(HentAdresseRequestTo.builder()
				.identifikator(hentForsendelseResponseTo.getMottaker().getMottakerId())
				.type(hentForsendelseResponseTo.getMottaker().getMottakerType())
				.build());
	}

	/**
	 * Her er rekkefølgen viktig. HentForsendelseResponseTo.dokumenter består av en ordnet liste av dokumenter i rekkefølgen HOVEDDOK, VEDLEGG1, VEDLEGG2, ...
	 * Denne rekkefølgen må bevares slik at bestillingen blir korrekt. Siden vi bruker List.java blir denne rekkefølgen ivaretatt
	 **/
	private List<DokdistDokument> getDocumentsFromS3(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.map(dokumentTo -> {
					String jsonPayload = storage.get(dokumentTo.getDokumentObjektReferanse())
							.orElseThrow(() -> new DokumentIkkeFunnetIS3Exception(format("Kunne ikke finne dokument i S3 med key=dokumentObjektReferanse=%s", dokumentTo
									.getDokumentObjektReferanse())));
					return deserializeS3JsonPayloadToDokdistDokument(jsonPayload, dokumentTo.getDokumentObjektReferanse());
				})
				.collect(Collectors.toList());
	}

	private DokdistDokument deserializeS3JsonPayloadToDokdistDokument(String jsonPayload, String objektReferanse) {
		DokdistDokument dokdistDokument;
		try {
			dokdistDokument = JsonSerializer.deserialize(jsonPayload, DokdistDokument.class);
			dokdistDokument.setDokumentObjektReferanse(objektReferanse);
		} catch (SdkClientException e) {
			throw new KunneIkkeDeserialisereS3JsonPayloadFunctionalException(format("Kunne ikke deserialisere jsonPayload fra s3 bucket for dokument med dokumentobjektreferanse=%s. Dokumentet er ikke persistert til s3 med korrekt format!", objektReferanse));
		}
		return dokdistDokument;
	}


}
