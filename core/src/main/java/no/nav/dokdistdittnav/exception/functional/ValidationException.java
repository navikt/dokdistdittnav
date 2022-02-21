package no.nav.dokdistdittnav.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class ValidationException extends AbstractDokdistdittnavFunctionalException {

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
