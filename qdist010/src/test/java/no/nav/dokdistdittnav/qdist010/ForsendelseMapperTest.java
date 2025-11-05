package no.nav.dokdistdittnav.qdist010;

import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.ArkivInformasjonTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.DokumentTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.MottakerTo;
import no.nav.tms.varsel.builder.BuilderEnvironment;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.AARSOPPGAVE_DOKUMENTTYPEID;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.AARSOPPGAVE_EPOSTVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.AARSOPPGAVE_EPOSTVARSLINGSTITTEL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.AARSOPPGAVE_SMSVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.ANNET_EPOSTVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.ANNET_EPOSTVARSLINGSTITTEL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.ANNET_SMSVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.ANNET_VARSELTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.SPRAAKKODE_BOKMAAL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VEDTAK_EPOSTVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VEDTAK_EPOSTVARSLINGSTITTEL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VEDTAK_SMSVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VEDTAK_VARSELTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VIKTIG_EPOSTVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VIKTIG_EPOSTVARSLINGSTITTEL;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VIKTIG_SMSVARSLINGSTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.VIKTIG_VARSELTEKST;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.opprettBeskjed;
import static no.nav.dokdistdittnav.qdist010.ForsendelseMapper.opprettOppgave;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ForsendelseMapperTest {

	private static final String VARSELTYPE_BESKJED = "beskjed";
	private static final String VARSELTYPE_OPPGAVE = "oppgave";
	private static final String BESTILLINGS_ID = "f7d9decd-95e6-4328-a8a6-e1208ce6772a";
	private static final String IDENT = "26478529286";
	private static final String FORSENDELSESTITTEL = "Informasjon om saksbehandlingstid uføretrygd";
	private static final String TEMA = "UFO";
	private static final String ARKIV_ID = "454030560";
	private static final String SENSITIVITET_SUBSTANTIAL = "substantial";
	private static final String DOKUMENTTYPE_ID = "U000001";

	@BeforeEach
	void setUp() {
		BuilderEnvironment.extend(Map.of(
				"NAIS_CLUSTER_NAME", "test-fss",
				"NAIS_NAMESPACE", "teamdokumenthandtering",
				"NAIS_APP_NAME", "dokdistdittnav")
		);
	}

	@ParameterizedTest
	@MethodSource
	public void skalLageBeskjed(String dokumenttypeId, String smsVarslingstekst, String epostVarslingstittel, String epostVarslingstekst) {

		HentForsendelseResponse forsendelse = createForsendelse(dokumenttypeId, ANNET);

		String beskjed = opprettBeskjed("https://url.no", forsendelse);

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

		assertThat(jsonResponse.getString("link")).isEqualTo("https://url.no/%s/%s".formatted(TEMA, ARKIV_ID));
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

	@ParameterizedTest
	@MethodSource
	public void skalLageOppgave(DistribusjonsTypeKode distribusjonstype, String varseltekst, String smsVarslingstekst, String epostVarslingstittel, String epostVarslingstekst) {
		HentForsendelseResponse forsendelse = createForsendelse(DOKUMENTTYPE_ID, distribusjonstype);

		String oppgave = opprettOppgave("https://url.no", forsendelse);

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

		assertThat(jsonResponse.getString("link")).isEqualTo("https://url.no/%s/%s".formatted(TEMA, ARKIV_ID));
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
				Arguments.of(VIKTIG, VIKTIG_VARSELTEKST, VIKTIG_SMSVARSLINGSTEKST, VIKTIG_EPOSTVARSLINGSTITTEL, VIKTIG_EPOSTVARSLINGSTEKST),
				Arguments.of(null, VIKTIG_VARSELTEKST, VIKTIG_SMSVARSLINGSTEKST, VIKTIG_EPOSTVARSLINGSTITTEL, VIKTIG_EPOSTVARSLINGSTEKST)
		);
	}

	@Test
	public void skalIkkeLageOppgaveForDistribusjonstypeAnnet() {
		HentForsendelseResponse forsendelse = createForsendelse(DOKUMENTTYPE_ID, ANNET);

		String oppgave = opprettOppgave("https://url.no", forsendelse);

		assertThat(oppgave).isBlank();
	}

	private HentForsendelseResponse createForsendelse(String dokumenttypeId, DistribusjonsTypeKode distribusjonstype) {
		return HentForsendelseResponse.builder()
				.bestillingsId(BESTILLINGS_ID)
				.forsendelseTittel(FORSENDELSESTITTEL)
				.distribusjonstype(distribusjonstype)
				.tema(TEMA)
				.mottaker(createMottaker())
				.arkivInformasjon(createArkivInformasjon())
				.dokumenter(singletonList(createDokument(dokumenttypeId)))
				.build();
	}

	private MottakerTo createMottaker() {
		return MottakerTo.builder()
				.mottakerId(IDENT)
				.build();
	}

	private ArkivInformasjonTo createArkivInformasjon() {
		return ArkivInformasjonTo.builder()
				.arkivId(ARKIV_ID)
				.build();
	}

	private DokumentTo createDokument(String dokumenttypeId) {
		return DokumentTo.builder()
				.tilknyttetSom("HOVEDDOKUMENT")
				.dokumentObjektReferanse("REFERANSE")
				.arkivDokumentInfoId("123")
				.dokumenttypeId(dokumenttypeId)
				.build();
	}

}