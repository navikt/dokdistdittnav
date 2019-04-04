package no.nav.dokdistdittnav.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class InvalidForsendelseStatusException extends AbstractDokdistdisttnavFunctionalException {

	public InvalidForsendelseStatusException(String message) {
		super(message);
	}

	public InvalidForsendelseStatusException(String message, Throwable cause) {
		super(message, cause);
	}
}
