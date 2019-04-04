package no.nav.dokdistdittnav.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public abstract class AbstractDokdistdisttnavFunctionalException extends RuntimeException {

	public AbstractDokdistdisttnavFunctionalException(String message) {
		super(message);
	}

	public AbstractDokdistdisttnavFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

}
