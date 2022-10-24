package no.nav.dokdistdittnav.consumer.dokarkiv;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.azure.AzureProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
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
	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	public DokarkivConsumer(WebClient webClient,
							DokdistdittnavProperties dokdistdittnavProperties,
							ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this.webClient = webClient;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;

	}

	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	@Monitor(value = DOKARKIV_CONSUMER, extraTags = {PROCESS, "oppdaterDistribusjonsinfo"}, histogram = true)
	public void settTidLestHoveddokument(JournalpostId journalpostId, OppdaterDistribusjonsInfo oppdaterDistribusjonsinfo) {
		webClient.patch()
				.uri(dokdistdittnavProperties.getDokarkiv().getOppdaterDistribusjonsinfoURI(journalpostId))
				.attributes(getOAuth2AuthorizedClient())
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
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(AzureProperties.getOAuth2AuthorizeRequestForAzure(AzureProperties.CLIENT_REGISTRATION_DOKARKIV));
		return ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(clientMono.block());
	}
}
