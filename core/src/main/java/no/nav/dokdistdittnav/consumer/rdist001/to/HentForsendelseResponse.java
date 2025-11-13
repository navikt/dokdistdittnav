package no.nav.dokdistdittnav.consumer.rdist001.to;

import lombok.Builder;
import lombok.Value;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonstidspunktKode;

import java.util.List;

import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.KLAR_FOR_DIST;

@Value
@Builder
public class HentForsendelseResponse {

	String bestillingsId;
	String konversasjonId;
	String bestillendeFagsystem;
	String modus;
	String forsendelseStatus;
	String distribusjonKanal;
	String tema;
	String forsendelseTittel;
	String batchId;
	String dokumentProdApp;
	MottakerTo mottaker;
	ArkivInformasjonTo arkivInformasjon;
	PostadresseTo postadresse;
	List<DokumentTo> dokumenter;
	DistribusjonsTypeKode distribusjonstype;
	DistribusjonstidspunktKode distribusjonstidspunkt;
	String varselStatus;

	@Value
	@Builder
	public static class MottakerTo {
		String mottakerId;
		String mottakerNavn;
		String mottakerType;
	}

	@Value
	@Builder
	public static class ArkivInformasjonTo {
		String arkivSystem;
		String arkivId;
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

	@Value
	@Builder
	public static class DokumentTo {
		String tilknyttetSom;
		String dokumentObjektReferanse;
		String arkivDokumentInfoId;
		String dokumenttypeId;
	}

	public boolean forsendelseHarUgyldigStatus() {
		return !erArkivertIJoark() || !KLAR_FOR_DIST.name().equals(forsendelseStatus);
	}

	// For at lenken til dokumentarkivet skal fungere må journalposten ligge i Joark
	public boolean erArkivertIJoark() {
		return arkivInformasjon != null
			   && arkivInformasjon.getArkivSystem().equals("JOARK")
			   && !arkivInformasjon.getArkivId().isBlank();
	}

	public boolean erDistribusjonstypeVedtakViktigEllerNull() {
		return distribusjonstype == null || distribusjonstype == VIKTIG || distribusjonstype == VEDTAK;
	}

	public boolean erDistribusjonstypeAnnet() {
		return distribusjonstype == ANNET;
	}

}