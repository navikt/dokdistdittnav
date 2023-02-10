package no.nav.dokdistdittnav.exception.functional;

public abstract class AbstractDokdistdittnavFunctionalException extends RuntimeException {

	public AbstractDokdistdittnavFunctionalException(String message) {
		super(message);
	}

	public AbstractDokdistdittnavFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

}
