package no.nav.dokdistdittnav.consumer.tkat020;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */
@Value
@Builder
public class DokumenttypeInfoTo {

	private final String dokumenttypeId;
	private final String dokumentTittel;
	private final boolean vedlegg;
	private final String ikkeRedigerbarMalId;
	private final String redigerbarMalId;
	private final String malLogikkFil;
	private final String malXsdReferanse;
	private final String dokumentKategori;
	private final boolean sensitivt;
	private final String predefinertDistKanal;
	private final String portoklasse;
	private final String konvoluttvinduType;
	private final String sentralPrintDokumentType;
	private final boolean tosidigprint;

}
