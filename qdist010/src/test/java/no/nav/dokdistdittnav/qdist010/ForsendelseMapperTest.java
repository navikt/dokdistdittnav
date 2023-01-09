package no.nav.dokdistdittnav.qdist010;

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_AARSOPPGAVE_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_VEDTAK_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_VIKTIG_TEKST;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VEDTAK_TITTEL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VIKTIG_TITTEL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.mapBeskjedIntern;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.oppretteOppgave;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForsendelseMapperTest {

	private static final String AARSOPPGAVE_ID = "000053";
	private static final String MELDING_FRA_NAV = "Melding fra NAV";
	private static final String AARSOPPGAVE_FRA_NAV = "Årsoppgave fra NAV";


	@ParameterizedTest
	@CsvSource(value = {
			"12345" + ", " + "varseltekster/melding_epostvarseltekst.html" + ", " + MELDING_FRA_NAV + ", " + SMS_TEKST,
			AARSOPPGAVE_ID + ", " + "varseltekster/aarsoppgave_epostvarseltekst.html" + ", " + AARSOPPGAVE_FRA_NAV + ", " + SMS_AARSOPPGAVE_TEKST
	})
	public void shouldMap(String dokumenttypeId, String epostVarslingstekstPath, String epostTittel, String smsVarslingstekst) {
		HentForsendelseResponseTo hentForsendelseResponseTo = createHentForsendelseResponteTo(dokumenttypeId, ANNET);
		BeskjedInput beskjedInput = mapBeskjedIntern("https://url.no", hentForsendelseResponseTo);


		assertEquals(beskjedInput.getEpostVarslingstekst(), classpathToString(epostVarslingstekstPath));
		assertEquals(beskjedInput.getEpostVarslingstittel(), epostTittel);
		assertEquals(beskjedInput.getSmsVarslingstekst(), smsVarslingstekst);
	}

	@ParameterizedTest
	@CsvSource(value = {
			"VIKTIG" + ", "+ "varseltekster/viktig_epostvarseltekst.html" + ", " + VIKTIG_TITTEL + ", " + "du har fått et brev som du må lese:" + ", " + SMS_VIKTIG_TEKST,
			"VEDTAK" + ", "+ "varseltekster/vedtak_epostvarseltekst.html" + ", " + VEDTAK_TITTEL + ", " + "du har fått et vedtak som gjelder:" + ", " + SMS_VEDTAK_TEKST
	})
	public void shouldMapOppgave(String kode, String epostPath, String expectedEpostTittel, String expectedTittel, String expectedSmsTekst) {
		HentForsendelseResponseTo hentForsendelseResponseTo = createHentForsendelseResponteTo("123456", DistribusjonsTypeKode.valueOf(kode));
		OppgaveInput oppgaveInput = oppretteOppgave("https://url.no", hentForsendelseResponseTo);


		assertThat(oppgaveInput.getTekst().contains(expectedTittel));
		assertEquals(oppgaveInput.getEpostVarslingstekst(), classpathToString(epostPath));
		assertEquals(oppgaveInput.getEpostVarslingstittel(), expectedEpostTittel);
		assertEquals(oppgaveInput.getSmsVarslingstekst(), expectedSmsTekst);
	}

	private HentForsendelseResponseTo createHentForsendelseResponteTo(String dokumenttypeId, DistribusjonsTypeKode kode) {
		return HentForsendelseResponseTo.builder()
				.forsendelseTittel("tittel")
				.distribusjonstype(kode)
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
