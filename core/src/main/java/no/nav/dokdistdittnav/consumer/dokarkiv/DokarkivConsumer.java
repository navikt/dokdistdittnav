package no.nav.dokdistdittnav.consumer.dokarkiv;

import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistdittnav.constants.MdcConstants.DOKARKIV_CONSUMER;
import static no.nav.dokdistdittnav.constants.MdcConstants.PROCESS;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;

@Slf4j
@Component
public class DokarkivConsumer {

	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final WebClient webClient;

	public DokarkivConsumer(DokdistdittnavProperties dokdistdittnavProperties,
							WebClient webClient) {
		this.webClient = webClient;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
	}

	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	@Monitor(value = DOKARKIV_CONSUMER, extraTags = {PROCESS, "oppdaterDistribusjonsinfo"}, histogram = true)
	public void settTidLestHoveddokument(JournalPostId journalPostId, OppdaterDistribusjonsInfo feilregistrerForsendelse) {
		webClient.patch()
				.uri(dokdistdittnavProperties.getDokarkiv().getOppdaterDistribusjonsinfoURI(journalPostId))
				.headers(this::createHeaders)
				.bodyValue(feilregistrerForsendelse)
				.retrieve()
				.bodyToMono(String.class)
				.doOnError(handleError(journalPostId))
				.block();
	}

	private Consumer<Throwable> handleError(JournalPostId journalPostId) {
		return error -> {
			if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
				log.error("Kall mot dokarkiv feilet funksjonelt ved registrering av lest status for journalpost med id={}, feilmelding={}", journalPostId.value(), error.getMessage());
				throw new DokarkivOppdaterDistribusjonsinfoFunctionalException(
						String.format("Kall mot SAF (GraphQL) feilet med status=%s, feilmelding=%s",
								response.getRawStatusCode(),
								response.getMessage()),
						error);
			} else {
				log.error("Kall mot dokarkiv feilet teknisk ved registrering av lest status for journalpost med id={}, feilmelding={}", journalPostId.value(), error.getMessage());
				throw new DokarkivOppdaterDistribusjonsinfoTechnicalException(
						String.format("Kall mot SAF (GraphQL) feilet med feilmelding=%s", error.getMessage()),
						error);
			}
		};
	}

	private void createHeaders(HttpHeaders headers) {
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(CALL_ID, MDC.get(CALL_ID));
	}
}
