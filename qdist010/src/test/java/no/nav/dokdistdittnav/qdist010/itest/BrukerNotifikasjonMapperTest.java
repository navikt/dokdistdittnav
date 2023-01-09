package no.nav.dokdistdittnav.qdist010.itest;

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_AARSOPPGAVE_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_TEKST;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BrukerNotifikasjonMapperTest {

	private static final String AARSOPPGAVE_ID = "000053";
	private static final String MELDING_FRA_NAV = "Melding fra NAV";
	private static final String AARSOPPGAVE_FRA_NAV = "Årsoppgave fra NAV";

	private final BrukerNotifikasjonMapper mapper = new BrukerNotifikasjonMapper();


	@ParameterizedTest
	@CsvSource(value = {
			"12345" + ", " + "varseltekster/melding_epostvarseltekst.html" + ", " + MELDING_FRA_NAV + ", " + SMS_TEKST,
			AARSOPPGAVE_ID + ", " + "varseltekster/aarsoppgave_epostvarseltekst.html" + ", " + AARSOPPGAVE_FRA_NAV + ", " + SMS_AARSOPPGAVE_TEKST
	})
	public void shouldMap(String dokumenttypeId, String epostVarslingstekstPath, String epostTittel, String smsVarslingstekst) {
		HentForsendelseResponseTo hentForsendelseResponseTo = createHentForsendelseResponteTo(dokumenttypeId);
		BeskjedInput beskjedInput = mapper.mapBeskjedIntern("https://url.no", hentForsendelseResponseTo);


		assertEquals(beskjedInput.getEpostVarslingstekst(), classpathToString(epostVarslingstekstPath));
		assertEquals(beskjedInput.getEpostVarslingstittel(), epostTittel);
		assertEquals(beskjedInput.getSmsVarslingstekst(), smsVarslingstekst);
	}

	private HentForsendelseResponseTo createHentForsendelseResponteTo(String dokumenttypeId) {
		return HentForsendelseResponseTo.builder()
				.forsendelseTittel("tittel")
				.tema("GEN")
				.arkivInformasjon(createArkivInformasjon())
				.dokumenter(Collections.singletonList(createDokument(dokumenttypeId)))
				.build();
	}

	private HentForsendelseResponseTo.ArkivInformasjonTo createArkivInformasjon() {
		return HentForsendelseResponseTo.ArkivInformasjonTo.builder()
				.arkivId("ARKIV").build();
	}

	private HentForsendelseResponseTo.DokumentTo createDokument(String dokumenttypeId) {
		return HentForsendelseResponseTo.DokumentTo.builder()
				.arkivDokumentInfoId("123")
				.dokumentObjektReferanse("REFERANSE")
				.tilknyttetSom("HOVEDDOKUMENT")
				.dokumenttypeId(dokumenttypeId).build();
	}
}
