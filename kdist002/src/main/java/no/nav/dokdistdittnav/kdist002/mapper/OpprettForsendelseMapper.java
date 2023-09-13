package no.nav.dokdistdittnav.kdist002.mapper;

import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseRequest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static no.nav.dokdistdittnav.utils.DokdistUtils.assertNotBlank;
import static no.nav.dokdistdittnav.utils.DokdistUtils.assertNotNull;
import static org.springframework.util.ObjectUtils.isEmpty;

public class OpprettForsendelseMapper {

	private static final String DISTRIBUSJON_KANAL_PRINT = "PRINT";
	private static final String DOKUMENTTYPE_ID = "U000001";
	private static final String HOVEDDOKUMENT = "HOVEDDOKUMENT";

	public OpprettForsendelseRequest map(HentForsendelseResponse hentForsendelseResponse, String nyBestillingsId) {

		if (hentForsendelseResponse == null) {
			throw new IllegalArgumentException("HentForsendelseResponse kan ikke være null");
		}

		assertThatAllRequiredFieldsArePresent(hentForsendelseResponse);
		AtomicReference<Integer> rekkefolge = new AtomicReference<>(2);

		return OpprettForsendelseRequest.builder()
				.bestillingsId(nyBestillingsId)
				.distribusjonsKanal(DISTRIBUSJON_KANAL_PRINT)
				.distribusjonstype(hentForsendelseResponse.getDistribusjonstype())
				.distribusjonstidspunkt(hentForsendelseResponse.getDistribusjonstidspunkt())
				.bestillendeFagsystem(hentForsendelseResponse.getBestillendeFagsystem())
				.tema(hentForsendelseResponse.getTema())
				.forsendelseTittel(hentForsendelseResponse.getForsendelseTittel())
				.batchId(hentForsendelseResponse.getBatchId())
				.dokumentProdApp(hentForsendelseResponse.getDokumentProdApp())
				.originalDistribusjonId(hentForsendelseResponse.getBestillingsId())
				.mottaker(mapMottakerTo(hentForsendelseResponse.getMottaker()))
				.arkivInformasjon(mapArkivInformasjonTo(hentForsendelseResponse.getArkivInformasjon()))
				.postadresse(mapPostadresse(hentForsendelseResponse.getPostadresse()))
				.dokumenter(hentForsendelseResponse.getDokumenter().stream()
						.map(dokumentTo -> {
							if (isHoveddokument(dokumentTo.getTilknyttetSom())) {
								return mapDokument(dokumentTo, 1);
							} else {
								OpprettForsendelseRequest.DokumentTo dok = mapDokument(dokumentTo, rekkefolge.get());
								rekkefolge.getAndSet(rekkefolge.get() + 1);
								return dok;
							}
						})
						.collect(Collectors.toList()))
				.build();
	}

	private OpprettForsendelseRequest.DokumentTo mapDokument(HentForsendelseResponse.DokumentTo dokumentTo, Integer rekkefolge) {
		return OpprettForsendelseRequest.DokumentTo.builder()
				.tilknyttetSom(dokumentTo.getTilknyttetSom())
				.dokumentObjektReferanse(dokumentTo.getDokumentObjektReferanse())
				.arkivDokumentInfoId(dokumentTo.getArkivDokumentInfoId())
				.rekkefolge(rekkefolge)
				.dokumenttypeId(DOKUMENTTYPE_ID)
				.build();
	}

	private OpprettForsendelseRequest.PostadresseTo mapPostadresse(HentForsendelseResponse.PostadresseTo postadresseTo) {
		return isEmpty(postadresseTo) ? null : OpprettForsendelseRequest.PostadresseTo.builder()
				.adresselinje1(postadresseTo.getAdresselinje1())
				.adresselinje2(postadresseTo.getAdresselinje2())
				.adresselinje3(postadresseTo.getAdresselinje3())
				.postnummer(postadresseTo.getPostnummer())
				.poststed(postadresseTo.getPoststed())
				.landkode(postadresseTo.getLandkode())
				.build();
	}

	private OpprettForsendelseRequest.ArkivInformasjonTo mapArkivInformasjonTo(HentForsendelseResponse.ArkivInformasjonTo arkivInformasjonTo) {
		return OpprettForsendelseRequest.ArkivInformasjonTo.builder()
				.arkivSystem(arkivInformasjonTo.getArkivSystem())
				.arkivId(arkivInformasjonTo.getArkivId())
				.build();
	}

	private OpprettForsendelseRequest.MottakerTo mapMottakerTo(HentForsendelseResponse.MottakerTo mottakerTo) {
		assertNotNull("Mottaker", mottakerTo);
		return OpprettForsendelseRequest.MottakerTo.builder()
				.mottakerId(mottakerTo.getMottakerId())
				.mottakerNavn(mottakerTo.getMottakerNavn())
				.mottakerType(mottakerTo.getMottakerType())
				.build();
	}

	private boolean isHoveddokument(String tilknyttetSom) {
		return HOVEDDOKUMENT.equals(tilknyttetSom);
	}

	private void assertThatAllRequiredFieldsArePresent(HentForsendelseResponse forsendelseResponse) {
		assertNotBlank("bestillingsId", forsendelseResponse.getBestillingsId());
		assertNotBlank("bestillendeFagsystem", forsendelseResponse.getBestillendeFagsystem());
		assertNotBlank("tema", forsendelseResponse.getTema());
		assertNotBlank("forsendelsetittel", forsendelseResponse.getForsendelseTittel());
		assertNotBlank("dokumentProdApp", forsendelseResponse.getDokumentProdApp());
		assertNotNull("Mottaker", forsendelseResponse.getMottaker());
		assertNotBlank("mottaker.mottakerId", forsendelseResponse.getMottaker().getMottakerId());
		assertNotBlank("mottaker.mottakerNavn", forsendelseResponse.getMottaker().getMottakerNavn());
		assertNotBlank("mottaker.mottakerType", forsendelseResponse.getMottaker().getMottakerType());

		if (forsendelseResponse.getArkivInformasjon() != null) {
			assertNotBlank("arkivinformasjon.arkivSystem", forsendelseResponse.getArkivInformasjon().getArkivSystem());
			assertNotBlank("arkivinformasjon.arkivId", forsendelseResponse.getArkivInformasjon().getArkivId());
		}

		if (forsendelseResponse.getPostadresse() != null) {
			assertNotBlank("postadresse.landkode", forsendelseResponse.getPostadresse().getLandkode());
		}

		assertThatAtLeastOneDocumentIsPresent(forsendelseResponse.getDokumenter());
		forsendelseResponse.getDokumenter().forEach(dokumentTo -> assertDokument(dokumentTo, forsendelseResponse.getArkivInformasjon()));
	}

	private void assertDokument(HentForsendelseResponse.DokumentTo dokumentTo, HentForsendelseResponse.ArkivInformasjonTo arkivInformasjonTo) {
		assertNotBlank("dokumenter.dokument.tilknyttetSom", dokumentTo.getTilknyttetSom());
		assertNotBlank("dokumenter.dokument.dokumentObjektReferanse", dokumentTo.getDokumentObjektReferanse());
		assertNotBlank("dokumenter.dokument.dokumenttypeId", dokumentTo.getDokumenttypeId());

		if (arkivInformasjonTo != null) {
			assertNotBlank("dokumenter.dokument.arkivdokumentInfoId", dokumentTo.getArkivDokumentInfoId());
		}
	}

	private void assertThatAtLeastOneDocumentIsPresent(List<HentForsendelseResponse.DokumentTo> dokumentToList) {
		if (dokumentToList == null || dokumentToList.isEmpty()) {
			throw new IllegalArgumentException("Ugyldig input: Feltet dokumenter må være en liste som inneholder minst ett dokumnet");
		}
	}
}
