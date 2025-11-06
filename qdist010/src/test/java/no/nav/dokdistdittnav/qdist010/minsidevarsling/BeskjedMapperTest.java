package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.AbstractVarselMapper.SPRAAKKODE_BOKMAAL;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.AARSOPPGAVE_DOKUMENTTYPEID;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.AARSOPPGAVE_EPOSTVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.AARSOPPGAVE_EPOSTVARSLINGSTITTEL;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.AARSOPPGAVE_SMSVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.ANNET_EPOSTVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.ANNET_EPOSTVARSLINGSTITTEL;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.ANNET_SMSVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.ANNET_VARSELTEKST;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.BeskjedMapper.opprettBeskjed;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.VarselService.lagLenkeMedTemaOgArkivId;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class BeskjedMapperTest extends AbstractVarselMapperTest {

	@ParameterizedTest
	@MethodSource
	public void skalLageBeskjed(String dokumenttypeId, String smsVarslingstekst, String epostVarslingstittel, String epostVarslingstekst) {
		HentForsendelseResponse forsendelse = createForsendelse(dokumenttypeId, ANNET);
		String lenkeTilVarsel = lagLenkeMedTemaOgArkivId(LENKE_DOKUMENTARKIV, forsendelse);

		String beskjed = opprettBeskjed(forsendelse, lenkeTilVarsel);

		var jsonResponse = new JSONObject(beskjed);

		assertThat(jsonResponse.getString("type")).isEqualTo(VARSELTYPE_BESKJED);
		assertThat(jsonResponse.getString("varselId")).isEqualTo(BESTILLINGS_ID);
		assertThat(jsonResponse.getString("ident")).isEqualTo(IDENT);

		var tekster = jsonResponse.getJSONArray("tekster");
		assertThat(tekster.length()).isEqualTo(1);
		assertThat(tekster.getJSONObject(0))
				.satisfies(it -> {
							assertThat(it.getString("spraakkode")).isEqualTo(SPRAAKKODE_BOKMAAL);
							assertThat(it.getString("tekst")).isEqualTo(ANNET_VARSELTEKST.formatted(FORSENDELSESTITTEL));
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

	private static Stream<Arguments> skalLageBeskjed() {
		return Stream.of(
				Arguments.of(DOKUMENTTYPE_ID, ANNET_SMSVARSLINGSTEKST, ANNET_EPOSTVARSLINGSTITTEL, ANNET_EPOSTVARSLINGSTEKST),
				Arguments.of(AARSOPPGAVE_DOKUMENTTYPEID, AARSOPPGAVE_SMSVARSLINGSTEKST, AARSOPPGAVE_EPOSTVARSLINGSTITTEL, AARSOPPGAVE_EPOSTVARSLINGSTEKST)
		);
	}

}