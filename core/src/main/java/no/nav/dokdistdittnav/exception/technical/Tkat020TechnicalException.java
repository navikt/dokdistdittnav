package no.nav.dokdistdittnav.exception.technical;

import org.springframework.web.client.HttpServerErrorException;

public class Tkat020TechnicalException extends AbstractDokdistdittnavTechnicalException {
	public Tkat020TechnicalException(String message) {
		super(message);
	}

	public Tkat020TechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
