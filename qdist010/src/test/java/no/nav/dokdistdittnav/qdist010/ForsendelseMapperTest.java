package no.nav.dokdistdittnav.qdist010;

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.ArkivInformasjonTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.DokumentTo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_AARSOPPGAVE_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VEDTAK_SMSTEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VIKTIG_SMSTEKST;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VEDTAK_EPOSTTITTEL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VIKTIG_EPOSTTITTEL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.opprettBeskjed;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.opprettOppgave;
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
	public void shouldMapBeskjed(String dokumenttypeId, String epostVarslingstekstPath, String epostTittel, String smsVarslingstekst) {
		HentForsendelseResponse forsendelse = createForsendelse(dokumenttypeId, ANNET);
		BeskjedInput beskjed = opprettBeskjed("https://url.no", forsendelse);

		assertEquals(beskjed.getEpostVarslingstekst(), classpathToString(epostVarslingstekstPath));
		assertEquals(beskjed.getEpostVarslingstittel(), epostTittel);
		assertEquals(beskjed.getSmsVarslingstekst(), smsVarslingstekst);
	}

	@ParameterizedTest
	@CsvSource(value = {
			"VIKTIG" + ", " + "varseltekster/viktig_epostvarseltekst.html" + ", " + VIKTIG_EPOSTTITTEL + ", " + "du har fått et brev som du må lese:" + ", " + VIKTIG_SMSTEKST,
			"VEDTAK" + ", " + "varseltekster/vedtak_epostvarseltekst.html" + ", " + VEDTAK_EPOSTTITTEL + ", " + "du har fått et vedtak som gjelder:" + ", " + VEDTAK_SMSTEKST
	})
	public void shouldMapOppgave(String kode, String epostPath, String expectedEpostTittel, String expectedTittel, String expectedSmsTekst) {
		HentForsendelseResponse forsendelse = createForsendelse("123456", DistribusjonsTypeKode.valueOf(kode));
		OppgaveInput oppgave = opprettOppgave("https://url.no", forsendelse);

		assertThat(oppgave.getTekst().contains(expectedTittel));
		assertEquals(oppgave.getEpostVarslingstekst(), classpathToString(epostPath));
		assertEquals(oppgave.getEpostVarslingstittel(), expectedEpostTittel);
		assertEquals(oppgave.getSmsVarslingstekst(), expectedSmsTekst);
	}

	@ParameterizedTest
	@CsvSource(value = {
			"varseltekster/viktig_epostvarseltekst.html" + ", " + VIKTIG_EPOSTTITTEL + ", " + "du har fått et brev som du må lese:" + ", " + VIKTIG_SMSTEKST
	})
	public void shouldMapOppgaveForDistribusjonstypeNull(String epostPath, String expectedEpostTittel, String expectedTittel, String expectedSmsTekst) {

		HentForsendelseResponse forsendelse = createForsendelse("123456", null);
		OppgaveInput oppgave = opprettOppgave("https://url.no", forsendelse);

		assertThat(oppgave.getTekst().contains(expectedTittel));
		assertEquals(oppgave.getEpostVarslingstekst(), classpathToString(epostPath));
		assertEquals(oppgave.getEpostVarslingstittel(), expectedEpostTittel);
		assertEquals(oppgave.getSmsVarslingstekst(), expectedSmsTekst);
	}

	@Test
	public void shouldNotMapOppgaveForDistribusjonstypeAnnet() {

		HentForsendelseResponse forsendelse = createForsendelse("123456", ANNET);
		OppgaveInput oppgave = opprettOppgave("https://url.no", forsendelse);

		assertThat(oppgave).isNull();
	}

	private HentForsendelseResponse createForsendelse(String dokumenttypeId, DistribusjonsTypeKode kode) {
		return HentForsendelseResponse.builder()
				.forsendelseTittel("tittel")
				.distribusjonstype(kode)
				.tema("GEN")
				.arkivInformasjon(createArkivInformasjon())
				.dokumenter(Collections.singletonList(createDokument(dokumenttypeId)))
				.build();
	}

	private ArkivInformasjonTo createArkivInformasjon() {
		return ArkivInformasjonTo.builder()
				.arkivId("ARKIV")
				.build();
	}

	private DokumentTo createDokument(String dokumenttypeId) {
		return DokumentTo.builder()
				.arkivDokumentInfoId("123")
				.dokumentObjektReferanse("REFERANSE")
				.tilknyttetSom("HOVEDDOKUMENT")
				.dokumenttypeId(dokumenttypeId)
				.build();
	}
}
