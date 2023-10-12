package no.nav.dokdistdittnav.consumer.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.azure.AzureProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import no.nav.dokdistdittnav.utils.NavHeadersFilter;
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

import static java.lang.String.format;
import static no.nav.dokdistdittnav.azure.AzureProperties.CLIENT_REGISTRATION_DOKNOTIFIKASJON;
import static no.nav.dokdistdittnav.constants.MdcConstants.DOKNOTIFIKASJON_CONSUMER;
import static no.nav.dokdistdittnav.constants.MdcConstants.PROCESS;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DoknotifikasjonConsumer {

	private final WebClient webClient;
	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	public DoknotifikasjonConsumer(WebClient webClient,
								   DokdistdittnavProperties dokdistdittnavProperties,
								   ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this.webClient = webClient.mutate()
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
		this.dokdistdittnavProperties = dokdistdittnavProperties;
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
	}

	@Monitor(value = DOKNOTIFIKASJON_CONSUMER, extraTags = {PROCESS, "_test_"}, histogram = true)
	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public NotifikasjonInfoTo getNotifikasjonInfo(String bestillingsId, boolean maaInkludereSendtDato) {
		return webClient.get()
				.uri(dokdistdittnavProperties.getDoknotifikasjon().getNotifikasjonInfoURI(bestillingsId))
				.attributes(getOAuth2AuthorizedClient())
				.retrieve()
				.bodyToMono(NotifikasjonInfoTo.class)
				.doOnError(handleError(bestillingsId))
				.doOnSuccess(notifikasjonInfoTo -> {
					if (maaInkludereSendtDato && notifikasjonInfoTo.notifikasjonDistribusjoner().stream()
							.anyMatch(it -> it.sendtDato() == null)) {
						throw new NotifikasjonManglerSendtDatoException(
								format("Notifikasjon med bestillingsId=%s inneholder en eller flere notifikasjonDistribusjoner uten sendtDato", bestillingsId));
					}
				})
				.block();
	}

	private Consumer<Throwable> handleError(String bestillingsId) {
		return error -> {
			if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
				log.error("Kall mot doknotifikasjon feilet funksjonelt ved henting av notifikasjon med bestillingsId={}, feilmelding={}", bestillingsId, error.getMessage());
				throw new DoknotifikasjonFunctionalException(
						format("Kall mot doknotifikasjon feilet funksjonelt ved henting av notifikasjon med bestillingsId=%s, status=%s, feilmelding=%s",
								bestillingsId,
								response.getStatusCode(),
								response.getMessage()),
						error);
			} else {
				log.error("Kall mot doknotifikasjon feilet teknisk ved henting av notifikasjon med bestillingsId={}, feilmelding={}", bestillingsId, error.getMessage());
				throw new DoknotifikasjonTechnicalException(
						format("Kall mot doknotifikasjon feilet teknisk ved henting av notifikasjon med bestillingsId=%s ,feilmelding=%s",
								bestillingsId,
								error.getMessage()),
						error);
			}
		};
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(AzureProperties.getOAuth2AuthorizeRequestForAzure(CLIENT_REGISTRATION_DOKNOTIFIKASJON));
		return ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(clientMono.block());
	}
}
