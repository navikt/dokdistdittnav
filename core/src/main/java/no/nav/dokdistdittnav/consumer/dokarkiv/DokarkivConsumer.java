package no.nav.dokdistdittnav.consumer.dokarkiv;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.utils.NavHeadersFilter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.azure.AzureProperties.CLIENT_REGISTRATION_DOKARKIV;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class DokarkivConsumer {

	private final WebClient webClient;

	public DokarkivConsumer(WebClient webClient,
							DokdistdittnavProperties dokdistdittnavProperties) {
		this.webClient = webClient.mutate()
				.filter(new NavHeadersFilter())
				.baseUrl(dokdistdittnavProperties.getDokarkiv().getBaseUri())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public void settDatoLest(String journalpostId, OppdaterDistribusjonsInfo oppdaterDistribusjonsinfo) {
		webClient.patch()
				.uri(uriBuilder -> uriBuilder
						.path("/{journalpostId}/oppdaterDistribusjonsinfo")
						.build(journalpostId))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.bodyValue(oppdaterDistribusjonsinfo)
				.retrieve()
				.bodyToMono(String.class)
				.onErrorMap(throwable -> mapError(throwable, journalpostId))
				.block();
	}

	private Throwable mapError(Throwable throwable, String journalpostId) {
		if (throwable instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			throw new DokarkivOppdaterDistribusjonsinfoFunctionalException(format("Kall mot dokarkiv feilet funksjonelt ved registrering av lest status for journalpost med id=%s, status=%s, feilmelding=%s",
					journalpostId,
					response.getStatusCode(),
					response.getMessage()),
					throwable);
		} else {
			throw new DokarkivOppdaterDistribusjonsinfoTechnicalException(format("Kall mot dokarkiv feilet teknisk ved registrering av lest status for journalpost med id=%s, feilmelding=%s",
					journalpostId,
					throwable.getMessage()),
					throwable);
		}
	}
}
