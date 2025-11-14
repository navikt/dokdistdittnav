package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.UgyldigVarsellenkeMinSideException;
import no.nav.dokdistdittnav.exception.functional.VarseltekstfilNotFoundException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

public abstract class AbstractVarselMapper {

	public static final int SYNLIGEDAGER = 10;
	public static final String SPRAAKKODE_BOKMAAL = "nb";

	public static String getFileAndAssertNotNullOrEmpty(String path) {
		String result = classpathToString(path);
		if (isEmpty(result)) {
			throw new VarseltekstfilNotFoundException("Fant ikke filen på path: " + path);
		}
		return result;
	}

	public static String lagLenkeTilVarsel(String url, HentForsendelseResponse forsendelse) {
		String tema = forsendelse.getTema();
		String arkivId = forsendelse.getArkivInformasjon().getArkivId();

		validerTema(tema);
		validerArkivId(arkivId);

		URI uri = UriComponentsBuilder
				.fromUriString(url)
				.path(tema + "/" + arkivId)
				.build().toUri();

		return uri.toString();
	}

	public static void validerTema(String tema) {
		if (tema == null || tema.isBlank()) {
			throw new UgyldigVarsellenkeMinSideException("Tema i lenke til Min Side er null eller tom.");
		}

		if (tema.length() != 3) {
			throw new UgyldigVarsellenkeMinSideException("Tema i lenke til Min Side har ikke 3 tegn. Mottok=%s".formatted(tema));
		}

		if (!tema.matches("[a-zA-Z]+")) {
			throw new UgyldigVarsellenkeMinSideException("Tema i lenke til Min Side består ikke av kun bokstaver. Mottok=%s".formatted(tema));
		}
	}

	public static void validerArkivId(String arkivId) {
		if (arkivId == null || arkivId.isBlank()) {
			throw new UgyldigVarsellenkeMinSideException("ArkivId i lenke til Min Side er null eller tom.");
		}

		if (!arkivId.matches("[0-9]+")) {
			throw new UgyldigVarsellenkeMinSideException("ArkivId i lenke til Min Side består ikke av kun tall. Mottok=%s".formatted(arkivId));
		}
	}

}