package no.nav.dokdistdittnav.qdist010;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import no.nav.dokdistdittnav.consumer.dokkat.tkat021.VarselInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.technical.KunneIkkeHenteDagensDatoTechnicalException;
import no.nav.dokdistdittnav.qdist010.map.DokumenthenvendelseMapper;
import no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.Dokumenthenvendelse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.GregorianCalendar;


/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
public class DokumenthenvendelseMapperTest {

	private static final String TEMA = "BIL";
	private static final String FORSENDELSE_TITTEL = "forsenselseTittel";
	private static final String MOTTAKER_ID = "mottakerId";
	private static final String ARKIV_ID = "arkivId";
	private static final String ARKIV_DOKUMENT_INFO_ID_1 = "arkiverDokumentInfoId";
	private static final String ARKIV_DOKUMENT_INFO_ID_2 = "arkiverDokumentInfoId";
	private static final String ARKIV_DOKUMENT_INFO_ID_3 = "arkiverDokumentInfoId";
	private static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";
	private static final String TILKNYTTET_SOM_VEDLEGG = "VEDLEGG";
	private static final Boolean STOPP_REPETERENDE_VARSEL = false;
	private static final String VARSEL_BESTILLING_ID = "varselBestillingId";
	private XMLGregorianCalendar FERDIGSTILL_DATO;

	private static DokumenthenvendelseMapper dokumenthenvendelseMapper = new DokumenthenvendelseMapper();


	@Test
	public void shouldMap() {
		FERDIGSTILL_DATO = getXMLGregorianCalendarNow();
		Dokumenthenvendelse dokumenthenvendelse = dokumenthenvendelseMapper.map(createHentForsendelseResponseTo(),
				createVarselInfoTo(),
				VARSEL_BESTILLING_ID,
				FERDIGSTILL_DATO
		);
		assertEquals(MOTTAKER_ID, dokumenthenvendelse.getPersonident());
		assertEquals(FORSENDELSE_TITTEL, dokumenthenvendelse.getDokumenttittel());
		assertEquals(TEMA, dokumenthenvendelse.getArkivtema().getValue());
		assertEquals(FERDIGSTILL_DATO, dokumenthenvendelse.getFerdigstiltDato());
		assertEquals(ARKIV_ID, dokumenthenvendelse.getJournalpostId());
		assertEquals(VARSEL_BESTILLING_ID, dokumenthenvendelse.getVarselbestillingId());
		assertEquals(STOPP_REPETERENDE_VARSEL, dokumenthenvendelse.isStoppRepeterendeVarsel());
		assertEquals(ARKIV_DOKUMENT_INFO_ID_1, dokumenthenvendelse.getDokumentIdListe().get(0));
		assertEquals(ARKIV_DOKUMENT_INFO_ID_2, dokumenthenvendelse.getDokumentIdListe().get(1));
		assertEquals(ARKIV_DOKUMENT_INFO_ID_3, dokumenthenvendelse.getDokumentIdListe().get(2));
	}

	private HentForsendelseResponseTo createHentForsendelseResponseTo() {
		return HentForsendelseResponseTo.builder()
				.tema(TEMA)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.mottaker(HentForsendelseResponseTo.MottakerTo.builder()
						.mottakerId(MOTTAKER_ID)
						.build())
				.arkivInformasjon(HentForsendelseResponseTo.ArkivInformasjonTo.builder()
						.arkivId(ARKIV_ID)
						.build())
				.dokumenter(Arrays.asList(HentForsendelseResponseTo.DokumentTo.builder()
								.arkivDokumentInfoId(ARKIV_DOKUMENT_INFO_ID_1)
								.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
								.build(),
						HentForsendelseResponseTo.DokumentTo.builder()
								.arkivDokumentInfoId(ARKIV_DOKUMENT_INFO_ID_2)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.build(),
						HentForsendelseResponseTo.DokumentTo.builder()
								.arkivDokumentInfoId(ARKIV_DOKUMENT_INFO_ID_3)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.build()))
				.build();
	}

	private VarselInfoTo createVarselInfoTo() {
		return VarselInfoTo.builder()
				.stoppRepeterendeVarsel(STOPP_REPETERENDE_VARSEL)
				.build();
	}

	private XMLGregorianCalendar getXMLGregorianCalendarNow() {
		XMLGregorianCalendar now;
		try {
			now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
		} catch (DatatypeConfigurationException e) {
			throw new KunneIkkeHenteDagensDatoTechnicalException("QDIST010 kunne ikke hente dagens dato", e);
		}
		return now;
	}


}
