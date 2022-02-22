package no.nav.dokdistdittnav.consumer.rdist001;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Data
@Builder
public class HentForsendelseResponseTo {
	private final String bestillingsId;
	private final String forsendelseStatus;
	private final String modus;
	private final String tema;
	private final String forsendelseTittel;
	private final MottakerTo mottaker;
	private final ArkivInformasjonTo arkivInformasjon;
	private final PostadresseTo postadresse;
	private final List<DokumentTo> dokumenter;
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

