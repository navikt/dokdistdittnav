package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.ArkivInformasjonTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.DokumentTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.MottakerTo;
import no.nav.tms.varsel.builder.BuilderEnvironment;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static java.util.Collections.singletonList;

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