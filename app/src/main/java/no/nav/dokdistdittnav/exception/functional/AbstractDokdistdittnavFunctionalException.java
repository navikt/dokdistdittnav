package no.nav.dokdistdittnav.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public abstract class AbstractDokdistdittnavFunctionalException extends RuntimeException {

	public AbstractDokdistdittnavFunctionalException(String message) {
		super(message);
	}

	public AbstractDokdistdittnavFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

}
