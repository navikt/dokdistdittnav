package no.nav.dokdistdittnav.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class ValidationException extends AbstractDokdistdisttnavFunctionalException {

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
