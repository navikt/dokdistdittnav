package no.nav.dokdistdittnav.qdist010.util;

import no.nav.dokdistdittnav.constants.DomainConstants;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.functional.InvalidForsendelseStatusException;

import java.util.stream.Collectors;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public final class Qdist009FunctionalUtils {
	private Qdist009FunctionalUtils() {
	}

	public static void validateForsendelseStatus(String forsendelseStatus) {
		if (!DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DIST.equals(forsendelseStatus)) {
			throw new InvalidForsendelseStatusException(String.format("ForsendelseStatus må være %s. Fant forsendelseStatus=%s", DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DIST, forsendelseStatus));
		}
	}

	public static String getDokumenttypeIdHoveddokument(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.filter(dokumentTo -> DomainConstants.HOVEDDOKUMENT.equals(dokumentTo.getTilknyttetSom()))
				.map(HentForsendelseResponseTo.DokumentTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}

}
