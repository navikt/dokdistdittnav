package no.nav.dokdistdittnav.consumer.rdist001.to;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonstidspunktKode;

import java.util.List;

@Data
@Builder
public class HentForsendelseResponseTo {
	private String bestillingsId;
	private String konversasjonId;
	private String bestillendeFagsystem;
	private String modus;
	private String forsendelseStatus;
	private String distribusjonKanal;
	private String tema;
	private String forsendelseTittel;
	private String batchId;
	private String dokumentProdApp;
	private MottakerTo mottaker;
	private ArkivInformasjonTo arkivInformasjon;
	private PostadresseTo postadresse;
	private List<DokumentTo> dokumenter;
	private DistribusjonsTypeKode distribusjonstype;
	private DistribusjonstidspunktKode distribusjonstidspunkt;
	private final String varselStatus;

	@Data
	@Builder
	public static class MottakerTo {
		private final String mottakerId;
		private final String mottakerNavn;
		private final String mottakerType;
	}

	@Data
	@Builder
	public static class ArkivInformasjonTo {
		private String arkivSystem;
		private final String arkivId;
	}

	@Value
	@Builder
	public static class PostadresseTo {
		String adresselinje1;
		String adresselinje2;
		String adresselinje3;
		String postnummer;
		String poststed;
		String landkode;
	}

	@Data
	@Builder
	public static class DokumentTo {
		private final String tilknyttetSom;
		private final String dokumentObjektReferanse;
		private final String arkivDokumentInfoId;
		private final String dokumenttypeId;
	}
}

