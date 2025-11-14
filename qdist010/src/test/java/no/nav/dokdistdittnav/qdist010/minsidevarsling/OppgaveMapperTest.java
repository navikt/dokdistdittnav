package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.FeilDistribusjonstypeForVarselTilMinSideException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.AbstractVarselMapper.SPRAAKKODE_BOKMAAL;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.VEDTAK_EPOSTVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.VEDTAK_EPOSTVARSLINGSTITTEL;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.VEDTAK_SMSVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.VEDTAK_VARSELTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.VIKTIG_ELLER_NULL_EPOSTVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.VIKTIG_ELLER_NULL_EPOSTVARSLINGSTITTEL;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.VIKTIG_ELLER_NULL_SMSVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.VIKTIG_ELLER_NULL_VARSELTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.OppgaveMapper.opprettOppgave;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OppgaveMapperTest extends AbstractVarselMapperTest {

	@ParameterizedTest
	@MethodSource
	public void skalLageOppgave(DistribusjonsTypeKode distribusjonstype, String varseltekst, String smsVarslingstekst, String epostVarslingstittel, String epostVarslingstekst) {
		HentForsendelseResponse forsendelse = createForsendelse(DOKUMENTTYPE_ID, distribusjonstype);

		String oppgave = opprettOppgave(forsendelse, LENKE_DOKUMENTARKIV);

		var jsonResponse = new JSONObject(oppgave);

		assertThat(jsonResponse.getString("type")).isEqualTo(VARSELTYPE_OPPGAVE);
		assertThat(jsonResponse.getString("varselId")).isEqualTo(BESTILLINGS_ID);
		assertThat(jsonResponse.getString("ident")).isEqualTo(IDENT);

		var tekster = jsonResponse.getJSONArray("tekster");
		assertThat(tekster.length()).isEqualTo(1);
		assertThat(tekster.getJSONObject(0))
				.satisfies(it -> {
							assertThat(it.getString("spraakkode")).isEqualTo(SPRAAKKODE_BOKMAAL);
							assertThat(it.getString("tekst")).isEqualTo(varseltekst.formatted(FORSENDELSESTITTEL));
							assertThat(it.getBoolean("default")).isEqualTo(true);
						}
				);

		assertThat(jsonResponse.getString("link")).isEqualTo(LENKE_DOKUMENTARKIV + "%s/%s".formatted(TEMA, ARKIV_ID));
		assertThat(jsonResponse.getString("sensitivitet")).isEqualTo(SENSITIVITET_SUBSTANTIAL);

		var aktivFremTil = jsonResponse.getString("aktivFremTil");
		assertThat(ZonedDateTime.parse(aktivFremTil).toInstant()).isCloseTo(ZonedDateTime.now().plusDays(10).toInstant(), within(1, SECONDS));

		var eksternVarsling = jsonResponse.getJSONObject("eksternVarsling");
		var prefererteKanaler = eksternVarsling.getJSONArray("prefererteKanaler");
		assertThat(prefererteKanaler.length()).isEqualTo(1);
		assertThat(prefererteKanaler.get(0)).isEqualTo("EPOST");
		assertThat(eksternVarsling.getString("smsVarslingstekst")).isEqualTo(smsVarslingstekst);
		assertThat(eksternVarsling.getString("epostVarslingstittel")).isEqualTo(epostVarslingstittel);
		assertThat(eksternVarsling.getString("epostVarslingstekst")).isEqualTo(epostVarslingstekst);
		assertThat(eksternVarsling.getBoolean("kanBatches")).isEqualTo(false);
	}

	private static Stream<Arguments> skalLageOppgave() {
		return Stream.of(
				Arguments.of(VEDTAK, VEDTAK_VARSELTEKST, VEDTAK_SMSVARSLINGSTEKST, VEDTAK_EPOSTVARSLINGSTITTEL, VEDTAK_EPOSTVARSLINGSTEKST),
				Arguments.of(VIKTIG, VIKTIG_ELLER_NULL_VARSELTEKST, VIKTIG_ELLER_NULL_SMSVARSLINGSTEKST, VIKTIG_ELLER_NULL_EPOSTVARSLINGSTITTEL, VIKTIG_ELLER_NULL_EPOSTVARSLINGSTEKST),
				Arguments.of(null, VIKTIG_ELLER_NULL_VARSELTEKST, VIKTIG_ELLER_NULL_SMSVARSLINGSTEKST, VIKTIG_ELLER_NULL_EPOSTVARSLINGSTITTEL, VIKTIG_ELLER_NULL_EPOSTVARSLINGSTEKST)
		);
	}

	@Test
	public void skalIkkeLageOppgaveForDistribusjonstypeAnnet() {
		HentForsendelseResponse forsendelse = createForsendelse(DOKUMENTTYPE_ID, ANNET);

		assertThatExceptionOfType(FeilDistribusjonstypeForVarselTilMinSideException.class)
				.isThrownBy(() -> opprettOppgave(forsendelse, LENKE_DOKUMENTARKIV))
				.withMessageContaining("Sender kun varsel med type=oppgave til Min Side for distribusjonstype VEDTAK, VIKTIG, eller null. Mottok=ANNET");
	}

}