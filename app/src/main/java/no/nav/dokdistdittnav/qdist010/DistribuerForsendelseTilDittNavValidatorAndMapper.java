package no.nav.dokdistdittnav.qdist010;

import static java.lang.String.format;

import no.nav.dokdistdittnav.exception.functional.ValidationException;
import no.nav.dokdistdittnav.qdist010.domain.DistribuerForsendelseTilSentralPrintTo;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class DistribuerForsendelseTilDittNavValidatorAndMapper {

	@Handler
	public DistribuerForsendelseTilSentralPrintTo ValidateAndMap(DistribuerForsendelseTilSentralPrint distribuerForsendelseTilSentralPrint) {
		assertNotNull(DistribuerForsendelseTilSentralPrint.class, distribuerForsendelseTilSentralPrint);
		assertNotNullOrEmpty("forsendelseId", distribuerForsendelseTilSentralPrint.getForsendelseId());
		return DistribuerForsendelseTilSentralPrintTo.builder()
				.forsendelseId(distribuerForsendelseTilSentralPrint.getForsendelseId())
				.build();
	}

	private void assertNotNullOrEmpty(String field, String value) {
		if (value == null || value.isEmpty()) {
			throw new ValidationException(format("Feltet %s kan ikke være null eller tomt. Fikk %s=%s", field, field, value));
		}
	}

	private void assertNotNull(Class inputClass, Object value) {
		if (value == null) {
			throw new ValidationException(format("%s kan ikke være null. Fikk %s=%s", inputClass.getCanonicalName(), inputClass
					.getCanonicalName(), value));
		}
	}
}
