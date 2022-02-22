package no.nav.dokdistdittnav.exception.technical;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public abstract class AbstractDokdistdittnavTechnicalException extends RuntimeException {

	public AbstractDokdistdittnavTechnicalException(String message) {
		super(message);
	}

	public AbstractDokdistdittnavTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
