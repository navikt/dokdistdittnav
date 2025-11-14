package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.ArkivInformasjonTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.DokumentTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.MottakerTo;
import no.nav.dokdistdittnav.exception.functional.UgyldigVarsellenkeMinSideException;
import no.nav.tms.varsel.builder.BuilderEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.AbstractVarselMapper.validerArkivId;
import static no.nav.dokdistdittnav.qdist010.minsidevarsling.AbstractVarselMapper.validerTema;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

abstract class AbstractVarselMapperTest {

	public static final String VARSELTYPE_BESKJED = "beskjed";
	public static final String VARSELTYPE_OPPGAVE = "oppgave";

	public static final String LENKE_DOKUMENTARKIV = "https://www.intern.test.nav.no/dokumentarkiv/tema/";

	public static final String BESTILLINGS_ID = "f7d9decd-95e6-4328-a8a6-e1208ce6772a";
	public static final String IDENT = "26478529286";
	public static final String FORSENDELSESTITTEL = "Informasjon om saksbehandlingstid uføretrygd";
	public static final String TEMA = "UFO";
	public static final String ARKIV_ID = "454030560";
	public static final String SENSITIVITET_SUBSTANTIAL = "substantial";
	public static final String DOKUMENTTYPE_ID = "U000001";

	@BeforeEach
	void setUp() {
		BuilderEnvironment.extend(Map.of(
				"NAIS_CLUSTER_NAME", "test-fss",
				"NAIS_NAMESPACE", "teamdokumenthandtering",
				"NAIS_APP_NAME", "dokdistdittnav")
		);
	}

	@Test
	void skalValidereTema() {
		assertThatNoException().isThrownBy(() -> validerTema(TEMA));
	}

	@ParameterizedTest
	@MethodSource
	void skalKasteExceptionForUgyldigTema(String tema, String feilmelding) {
		assertThatExceptionOfType(UgyldigVarsellenkeMinSideException.class)
				.isThrownBy(() -> validerTema(tema))
				.withMessage(feilmelding);
	}

	private static Stream<Arguments> skalKasteExceptionForUgyldigTema() {
		return Stream.of(
				Arguments.of(null, "Tema i lenke til Min Side er null eller tom."),
				Arguments.of("", "Tema i lenke til Min Side er null eller tom."),
				Arguments.of(" ", "Tema i lenke til Min Side er null eller tom."),
				Arguments.of("FA", "Tema i lenke til Min Side har ikke 3 tegn. Mottok=FA"),
				Arguments.of("FARA", "Tema i lenke til Min Side har ikke 3 tegn. Mottok=FARA"),
				Arguments.of("F1R", "Tema i lenke til Min Side består ikke av kun bokstaver. Mottok=F1R"),
				Arguments.of(" FA", "Tema i lenke til Min Side består ikke av kun bokstaver. Mottok= FA")
		);
	}

	@Test
	void skalValidereArkivId() {
		assertThatNoException().isThrownBy(() -> validerArkivId(ARKIV_ID));
	}

	@ParameterizedTest
	@MethodSource
	void skalKasteExceptionForUgyldigArkivId(String arkivId, String feilmelding) {
		assertThatExceptionOfType(UgyldigVarsellenkeMinSideException.class)
				.isThrownBy(() -> validerArkivId(arkivId))
				.withMessage(feilmelding);
	}

	private static Stream<Arguments> skalKasteExceptionForUgyldigArkivId() {
		return Stream.of(
				Arguments.of(null, "ArkivId i lenke til Min Side er null eller tom."),
				Arguments.of("", "ArkivId i lenke til Min Side er null eller tom."),
				Arguments.of(" ", "ArkivId i lenke til Min Side er null eller tom."),
				Arguments.of("12345a", "ArkivId i lenke til Min Side består ikke av kun tall. Mottok=12345a"),
				Arguments.of("a23456", "ArkivId i lenke til Min Side består ikke av kun tall. Mottok=a23456")
		);
	}

	public HentForsendelseResponse createForsendelse(String dokumenttypeId, DistribusjonsTypeKode distribusjonstype) {
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

	public MottakerTo createMottaker() {
		return MottakerTo.builder()
				.mottakerId(IDENT)
				.build();
	}

	public ArkivInformasjonTo createArkivInformasjon() {
		return ArkivInformasjonTo.builder()
				.arkivId(ARKIV_ID)
				.build();
	}

	public DokumentTo createDokument(String dokumenttypeId) {
		return DokumentTo.builder()
				.tilknyttetSom("HOVEDDOKUMENT")
				.dokumentObjektReferanse("REFERANSE")
				.arkivDokumentInfoId("123")
				.dokumenttypeId(dokumenttypeId)
				.build();
	}

}