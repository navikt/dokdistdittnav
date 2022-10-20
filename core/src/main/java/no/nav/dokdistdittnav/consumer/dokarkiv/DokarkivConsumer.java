package no.nav.dokdistdittnav.consumer.dokarkiv;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.azure.AzureTokenConsumer;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import static no.nav.dokdistdittnav.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistdittnav.constants.MdcConstants.DOKARKIV_CONSUMER;
import static no.nav.dokdistdittnav.constants.MdcConstants.PROCESS;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;

@Slf4j
@Component
public class DokarkivConsumer {

	private final WebClient webClient;
	private final AzureTokenConsumer azureTokenConsumer;
	private final DokdistdittnavProperties dokdistdittnavProperties;

	public DokarkivConsumer(WebClient webClient,
							AzureTokenConsumer azureTokenConsumer,
							DokdistdittnavProperties dokdistdittnavProperties) {
		this.webClient = webClient;
		this.azureTokenConsumer = azureTokenConsumer;
		this.dokdistdittnavProperties = dokdistdittnavProperties;

	}

	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	@Monitor(value = DOKARKIV_CONSUMER, extraTags = {PROCESS, "oppdaterDistribusjonsinfo"}, histogram = true)
	public void settTidLestHoveddokument(JournalpostId journalpostId, OppdaterDistribusjonsInfo oppdaterDistribusjonsinfo) {
		webClient.patch()
				.uri(dokdistdittnavProperties.getDokarkiv().getOppdaterDistribusjonsinfoURI(journalpostId))
				.headers(this::createHeaders)
				.bodyValue(oppdaterDistribusjonsinfo)
				.retrieve()
				.bodyToMono(String.class)
				.doOnError(handleError(journalpostId))
				.block();
	}

	private Consumer<Throwable> handleError(JournalpostId journalpostId) {
		return error -> {
			if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
				log.error("Kall mot dokarkiv feilet funksjonelt ved registrering av lest status for journalpost med id={}, feilmelding={}", journalpostId.value(), error.getMessage());
				throw new DokarkivOppdaterDistribusjonsinfoFunctionalException(
						String.format("Kall mot dokarkiv feilet funksjonelt ved registrering av lest status for journalpost med id=%s, status=%s, feilmelding=%s",
								journalpostId.value(),
								response.getRawStatusCode(),
								response.getMessage()),
						error);
			} else {
				log.error("Kall mot dokarkiv feilet teknisk ved registrering av lest status for journalpost med id={}, feilmelding={}", journalpostId.value(), error.getMessage());
				throw new DokarkivOppdaterDistribusjonsinfoTechnicalException(
						String.format("Kall mot dokarkiv feilet teknisk ved registrering av lest status for journalpost med id=%s ,feilmelding=%s",
								journalpostId.value(),
								error.getMessage()),
						error);
			}
		};
	}

	private void createHeaders(HttpHeaders headers) {
		headers.set(CALL_ID, MDC.get(CALL_ID));
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(azureTokenConsumer.getClientCredentialToken(dokdistdittnavProperties.getDokarkiv().getOauthScope()).getAccess_token());
	}
}
