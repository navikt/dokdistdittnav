package no.nav.dokdistdittnav.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class ForsendelseValidationException extends AbstractDokdistdittnavFunctionalException {

	public ForsendelseValidationException(String message) {
		super(message);
	}

	public ForsendelseValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
