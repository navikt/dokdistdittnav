package no.nav.dokdistdittnav.kdist002;

import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseRequestTo;
import no.nav.dokdistdittnav.kdist002.mapper.PersisterForsendelseMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersisterForsendelseMapperTest {

	private static final String BESTILLINGS_ID = UUID.randomUUID().toString();
	private static final String OLD_BESTILLINGS_ID = UUID.randomUUID().toString();
	private static final String BATCH_ID = "batchId";
	private static final String BESTILLENDE_FAGSYSTEM = "bestillendeFagsystem";
	private static final String TEMA = "FS22";
	private static final String FORSENDELSE_TITTEL = "forsendelseTittel";
	private static final String ARKIV_SYSTEM = "JOARK";
	private static final String ARKIV_ID = "arkivId";
	private static final String MOTTAKER_ID_NAVN = "mottakerIdNavn";
	private static final String MOTTAKER_ID = "mottakerId";
	private static final String ADRESSELINJE_1 = "adresselinje1";
	private static final String ADRESSELINJE_2 = "adresselinje2";
	private static final String ADRESSELINJE_3 = "adresselinje3";
	private static final String POSTNUMMER = "postnummer";
	private static final String POSTSTED = "poststed";
	private static final String LAND = "land";
	private static final String DOKUMENT_PROD_APP = "dokumentProdApp";
	private static final String DOKUMENTTYPE_ID_1 = "U000001";
	private static final String DOKUMENTTYPE_ID_2 = "U000001";
	private static final String OBJEKT_REFERANSE_1 = "objektReferanse1";
	private static final String OBJEKT_REFERANSE_2 = "objektReferanse2";
	private static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";
	private static final String TILKNYTTET_SOM_VEDLEGG = "VEDLEGG";
	private static final String ARKIV_DOKUMENTINFO_ID_1 = "arkivDokumentinfoId1";
	private static final String ARKIV_DOKUMENTINFO_ID_2 = "arkivDokumentinfoId2";


	private final PersisterForsendelseMapper mapper = new PersisterForsendelseMapper();

	@Test
	public void shouldMapForsendelser() {
		PersisterForsendelseRequestTo request = mapper.map(createHentForsendelseResponse(), BESTILLINGS_ID);

		assertEquals(request.getBestillingsId(), BESTILLINGS_ID);
		assertEquals(request.getForsendelseTittel(), FORSENDELSE_TITTEL);
		assertEquals(request.getBatchId(), BATCH_ID);
		assertEquals(request.getDokumentProdApp(), DOKUMENT_PROD_APP);
		assertEquals(request.getBestillendeFagsystem(), BESTILLENDE_FAGSYSTEM);
		assertEquals(request.getArkivInformasjon().getArkivId(), ARKIV_ID);
		assertEquals(request.getMottaker().getMottakerId(), MOTTAKER_ID);
		assertEquals(request.getMottaker().getMottakerNavn(), MOTTAKER_ID_NAVN);
		assertEquals(request.getOriginalDistribusjonId(), OLD_BESTILLINGS_ID);
		assertPostadresseTo(request.getPostadresse());
		assertDokument(request.getDokumenter().get(1));

	}

	@Test
	public void shouldMapForsendelserWhenAdresseErNull() {
		HentForsendelseResponseTo hentForsendelseResponse = createHentForsendelseResponse();
		hentForsendelseResponse.setPostadresse(null);
		PersisterForsendelseRequestTo request = mapper.map(hentForsendelseResponse, BESTILLINGS_ID);

		assertEquals(request.getBestillingsId(), BESTILLINGS_ID);
		assertEquals(request.getForsendelseTittel(), FORSENDELSE_TITTEL);
		assertEquals(request.getBatchId(), BATCH_ID);
		assertEquals(request.getDokumentProdApp(), DOKUMENT_PROD_APP);
		assertEquals(request.getBestillendeFagsystem(), BESTILLENDE_FAGSYSTEM);
		assertEquals(request.getArkivInformasjon().getArkivId(), ARKIV_ID);
		assertEquals(request.getMottaker().getMottakerId(), MOTTAKER_ID);
		assertEquals(request.getMottaker().getMottakerNavn(), MOTTAKER_ID_NAVN);
		assertEquals(request.getOriginalDistribusjonId(), OLD_BESTILLINGS_ID);
		assertNull(request.getPostadresse());
		assertDokument(request.getDokumenter().get(1));

	}

	@Test
	public void shouldThrowExceptionIfHentForsendelseResponseIsNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(null, BESTILLINGS_ID));
		assertEquals(exception.getMessage(), "HentForsendelseResponseTo kan ikke være null");
	}

	@Test
	public void shouldThrowExceptionIfTemaBlank() {
		HentForsendelseResponseTo hentForsendelseResponse = createHentForsendelseResponse();
		hentForsendelseResponse.setTema(null);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> mapper.map( hentForsendelseResponse, null));
		assertEquals(exception.getMessage(), "tema kan ikke være null");
	}

	@Test
	public void shouldThrowExceptionIfMottakerIsNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(createHentForsendelseResponseWithMottakerNull(), BESTILLINGS_ID));
		assertEquals(exception.getMessage(), "Mottaker kan ikke være null");
	}

	private void assertPostadresseTo(PersisterForsendelseRequestTo.PostadresseTo postadresse) {
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getPostnummer(), POSTNUMMER);
		assertEquals(postadresse.getPoststed(), POSTSTED);
		assertEquals(postadresse.getLandkode(), LAND);
	}

	private void assertDokument(PersisterForsendelseRequestTo.DokumentTo dokumentTo) {

		assertEquals(dokumentTo.getDokumenttypeId(), DOKUMENTTYPE_ID_2);
		assertEquals(dokumentTo.getDokumentObjektReferanse(), OBJEKT_REFERANSE_2);
		assertEquals(dokumentTo.getTilknyttetSom(), TILKNYTTET_SOM_VEDLEGG);
		assertEquals(dokumentTo.getRekkefolge(), 2);
		assertEquals(dokumentTo.getArkivDokumentInfoId(), ARKIV_DOKUMENTINFO_ID_2);


	}

	private HentForsendelseResponseTo createHentForsendelseResponseWithMottakerNull() {
		return HentForsendelseResponseTo.builder()
				.bestillingsId(OLD_BESTILLINGS_ID)
				.tema(TEMA)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.batchId(BATCH_ID)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.arkivInformasjon(HentForsendelseResponseTo.ArkivInformasjonTo.builder()
						.arkivId(ARKIV_ID).build())
				.mottaker(null)
				.postadresse(createPostadresse())
				.dokumenter(createDokument()).build();
	}

	private HentForsendelseResponseTo createHentForsendelseResponse() {
		return HentForsendelseResponseTo.builder()
				.bestillingsId(OLD_BESTILLINGS_ID)
				.tema(TEMA)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.batchId(BATCH_ID)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.arkivInformasjon(HentForsendelseResponseTo.ArkivInformasjonTo.builder()
						.arkivSystem(ARKIV_SYSTEM)
						.arkivId(ARKIV_ID).build())
				.mottaker(createMottakerTo())
				.postadresse(createPostadresse())
				.dokumenter(createDokument()).build();
	}

	private List<HentForsendelseResponseTo.DokumentTo> createDokument() {

		return Arrays.asList(
				HentForsendelseResponseTo.DokumentTo.builder()
						.dokumenttypeId(DOKUMENTTYPE_ID_1)
						.dokumentObjektReferanse(OBJEKT_REFERANSE_1)
						.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
						.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
						.build(),
				HentForsendelseResponseTo.DokumentTo.builder()
						.dokumenttypeId(DOKUMENTTYPE_ID_2)
						.dokumentObjektReferanse(OBJEKT_REFERANSE_2)
						.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
						.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2)
						.build(),
				HentForsendelseResponseTo.DokumentTo.builder()
						.dokumenttypeId("1234")
						.dokumentObjektReferanse(OBJEKT_REFERANSE_1)
						.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
						.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
						.build());


	}

	private HentForsendelseResponseTo.PostadresseTo createPostadresse() {
		return HentForsendelseResponseTo.PostadresseTo.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(LAND)
				.build();
	}

	private HentForsendelseResponseTo.MottakerTo createMottakerTo() {
		return HentForsendelseResponseTo.MottakerTo.builder()
				.mottakerNavn(MOTTAKER_ID_NAVN)
				.mottakerId(MOTTAKER_ID)
				.mottakerType("PERSON")
				.build();
	}

}