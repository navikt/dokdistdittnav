package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.exception.functional.VarseltekstfilNotFoundException;

import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

public abstract class AbstractVarselMapper {

	public static final Integer SYNLIGEDAGER = 10;
	public static final String SPRAAKKODE_BOKMAAL = "nb";

	public static String getFileAndAssertNotNullOrEmpty(String path) {
		String result = classpathToString(path);
		if (isEmpty(result)) {
			throw new VarseltekstfilNotFoundException("Fant ikke filen på path: " + path);
		}
		return result;
	}

}