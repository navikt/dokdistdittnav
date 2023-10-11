package no.nav.dokdistdittnav.consumer.doknotifikasjon;

import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;

public class NotifikasjonManglerSendtDatoException extends AbstractDokdistdittnavTechnicalException {

	public NotifikasjonManglerSendtDatoException(String message) {
		super(message);
	}

}