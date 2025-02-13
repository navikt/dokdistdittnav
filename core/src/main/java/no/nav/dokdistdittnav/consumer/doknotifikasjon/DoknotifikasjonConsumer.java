package no.nav.dokdistdittnav.consumer.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import no.nav.dokdistdittnav.utils.NavHeadersFilter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;
import reactor.core.publisher.Mono;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.azure.AzureProperties.CLIENT_REGISTRATION_DOKNOTIFIKASJON;
import static no.nav.dokdistdittnav.constants.MdcConstants.DOKNOTIFIKASJON_CONSUMER;
import static no.nav.dokdistdittnav.constants.MdcConstants.PROCESS;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class DoknotifikasjonConsumer {

	private final WebClient webClient;
	private final DokdistdittnavProperties dokdistdittnavProperties;

	public DoknotifikasjonConsumer(WebClient webClient,
								   DokdistdittnavProperties dokdistdittnavProperties) {
		this.webClient = webClient.mutate()
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
		this.dokdistdittnavProperties = dokdistdittnavProperties;
	}

	@Monitor(value = DOKNOTIFIKASJON_CONSUMER, extraTags = {PROCESS, "_test_"}, histogram = true)
	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public NotifikasjonInfoTo getNotifikasjonInfo(String bestillingsId, boolean maaInkludereSendtDato) {
		return webClient.get()
				.uri(dokdistdittnavProperties.getDoknotifikasjon().getNotifikasjonInfoURI(bestillingsId))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKNOTIFIKASJON))
				.retrieve()
				.bodyToMono(NotifikasjonInfoTo.class)
				.flatMap(notifikasjonInfoTo -> {
					if (maaInkludereSendtDato && notifikasjonInfoTo.notifikasjonDistribusjoner().stream()
							.anyMatch(it -> it.sendtDato() == null)) {
						return Mono.error(new NotifikasjonManglerSendtDatoException(
								format("Notifikasjon med bestillingsId=%s inneholder en eller flere notifikasjonDistribusjoner uten sendtDato", bestillingsId)));
					}
					return Mono.just(notifikasjonInfoTo);
				})
				.onErrorMap(throwable -> mapError(throwable, bestillingsId))
				.block();
	}

	private Throwable mapError(Throwable error, String bestillingsId) {
		if (error instanceof WebClientResponseException webException) {
			if (error instanceof NotFound notFound) {
				String notFoundErrorMsg = format("Fant ikke bestillingsId=%s i doknotifikasjon. Forsøker på nytt", bestillingsId);
				return new DoknotifikasjonTechnicalException(notFoundErrorMsg, notFound);
			} else if (webException.getStatusCode().is4xxClientError()) {
				String clientErrorMsg = format("Kall mot doknotifikasjon feilet funksjonelt ved henting av notifikasjon med bestillingsId=%s, status=%s, feilmelding=%s",
						bestillingsId, webException.getStatusCode(), webException.getMessage());
				return new DoknotifikasjonFunctionalException(clientErrorMsg, error);
			} else {
				String serverErrorMsg = format("Kall mot doknotifikasjon feilet teknisk ved henting av notifikasjon med bestillingsId=%s, status=%s ,feilmelding=%s",
						bestillingsId, webException.getStatusCode(), error.getMessage());
				return new DoknotifikasjonTechnicalException(serverErrorMsg, webException);
			}
		} else {
			String ukjentErrorMsg = format("Kall mot doknotifikasjon feilet med ukjent teknisk feil ved henting av notifikasjon med bestillingsId=%s ,feilmelding=%s. Se stacktrace",
					bestillingsId, error.getMessage());
			return new DoknotifikasjonTechnicalException(ukjentErrorMsg, error);
		}
	}
}
