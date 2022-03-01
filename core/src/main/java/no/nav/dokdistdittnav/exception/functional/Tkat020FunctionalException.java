package no.nav.dokdistdittnav.exception.functional;

import org.springframework.web.client.HttpClientErrorException;

public class Tkat020FunctionalException extends AbstractDokdistdittnavFunctionalException {
	public Tkat020FunctionalException(String message) {
		super(message);
	}

	public Tkat020FunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
