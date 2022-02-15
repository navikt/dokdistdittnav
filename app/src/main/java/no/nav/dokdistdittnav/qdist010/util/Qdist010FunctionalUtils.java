package no.nav.dokdistdittnav.qdist010.util;

import com.amazonaws.util.IOUtils;
import lombok.SneakyThrows;
import no.nav.dokdistdittnav.constants.DomainConstants;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.functional.InvalidForsendelseStatusException;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public final class Qdist010FunctionalUtils {
	private Qdist010FunctionalUtils() {
	}

	public static void validateForsendelseStatus(String forsendelseStatus) {
		if (!DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DIST.equals(forsendelseStatus)) {
			throw new InvalidForsendelseStatusException(format("ForsendelseStatus må være %s. Fant forsendelseStatus=%s", DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DIST, forsendelseStatus));
		}
	}

	public static String getDokumenttypeIdHoveddokument(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.filter(dokumentTo -> DomainConstants.HOVEDDOKUMENT.equals(dokumentTo.getTilknyttetSom()))
				.map(HentForsendelseResponseTo.DokumentTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}

	@SneakyThrows
	public static String classpathToString(String path) {
		try {
			InputStream inputStream = new ClassPathResource(path).getInputStream();
			String message = com.amazonaws.util.IOUtils.toString(inputStream);
			IOUtils.closeQuietly(inputStream, null);
			return message;
		} catch (IOException e) {
			throw new IOException(format("Kunne ikke åpne classpath-ressurs %s", path), e);
		}
	}

}
