package no.nav.dokdistdittnav.consumer.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.azure.AzureProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.dokarkiv.DokarkivOppdaterDistribusjonsinfoFunctionalException;
import no.nav.dokdistdittnav.consumer.dokarkiv.DokarkivOppdaterDistribusjonsinfoTechnicalException;
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

import static no.nav.dokdistdittnav.constants.MdcConstants.DOKNOTIFIKASJON_CONSUMER;
import static no.nav.dokdistdittnav.constants.MdcConstants.MDC_CALL_ID;
import static no.nav.dokdistdittnav.constants.MdcConstants.PROCESS;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;

@Slf4j
@Component
public class DoknotifikasjonConsumer {

	private final WebClient webClient;
	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	public DoknotifikasjonConsumer(WebClient webClient,
								   DokdistdittnavProperties dokdistdittnavProperties,
								   ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this.webClient = webClient;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
	}

	@Monitor(value = DOKNOTIFIKASJON_CONSUMER, extraTags = {PROCESS, "_test_"}, histogram = true)
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public NotifikasjonInfoTo getNotifikasjonInfo(String bestillingsId) {
		return webClient.get()
				.uri(dokdistdittnavProperties.getDoknotifikasjon().getNotifikasjonInfoURI(bestillingsId))
				.attributes(getOAuth2AuthorizedClient())
				.headers(this::createHeaders)
				.retrieve()
				.bodyToMono(NotifikasjonInfoTo.class)
				.doOnError(handleError(bestillingsId))
				.block();
	}

	private Consumer<Throwable> handleError(String bestillingsId) {
		return error -> {
			if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
				log.error("Kall mot doknotifikasjon feilet funksjonelt ved henting av notifikasjon med bestillingsId={}, feilmelding={}", bestillingsId, error.getMessage());
				throw new DokarkivOppdaterDistribusjonsinfoFunctionalException(
						String.format("Kall mot doknotifikasjon feilet funksjonelt ved henting av notifikasjon med bestillingsId=%s, status=%s, feilmelding=%s",
								bestillingsId,
								response.getRawStatusCode(),
								response.getMessage()),
						error);
			} else {
				log.error("Kall mot doknotifikasjon feilet teknisk ved henting av notifikasjon med bestillingsId={}, feilmelding={}", bestillingsId, error.getMessage());
				throw new DokarkivOppdaterDistribusjonsinfoTechnicalException(
						String.format("Kall mot doknotifikasjon feilet teknisk ved henting av notifikasjon med bestillingsId=%s ,feilmelding=%s",
								bestillingsId,
								error.getMessage()),
						error);
			}
		};
	}


	private void createHeaders(HttpHeaders headers) {
		headers.set(MDC_CALL_ID, MDC.get(MDC_CALL_ID));
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(AzureProperties.getOAuth2AuthorizeRequestForAzure(AzureProperties.CLIENT_REGISTRATION_DOKNOTIFIKASJON));
		return ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(clientMono.block());
	}
}
