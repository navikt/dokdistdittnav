package no.nav.dokdistdittnav.exception.technical;

public class KafkaTechnicalException extends AbstractDokdistdittnavTechnicalException {
	public KafkaTechnicalException(String message) {
		super(message);
	}

	public KafkaTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
