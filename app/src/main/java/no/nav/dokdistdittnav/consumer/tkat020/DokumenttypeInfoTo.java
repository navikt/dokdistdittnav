package no.nav.dokdistdittnav.consumer.tkat020;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */
@Value
@Builder
public class DokumenttypeInfoTo {

	private final String portoklasse;
	private final String konvoluttvinduType;
	private final String sentralPrintDokumentType;
	private final boolean tosidigprint;
}
