package no.nav.dokdistdittnav.consumer.dokarkiv;

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

import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.azure.AzureProperties.CLIENT_REGISTRATION_DOKARKIV;
import static no.nav.dokdistdittnav.constants.MdcConstants.DOK_CONSUMER;
import static no.nav.dokdistdittnav.constants.MdcConstants.PROCESS;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class DokarkivConsumer {

	private final WebClient webClient;
	private final DokdistdittnavProperties dokdistdittnavProperties;

	public DokarkivConsumer(WebClient webClient,
							DokdistdittnavProperties dokdistdittnavProperties) {
		this.webClient = webClient.mutate()
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
		this.dokdistdittnavProperties = dokdistdittnavProperties;
	}

	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	@Monitor(value = DOK_CONSUMER, extraTags = {PROCESS, "oppdaterDistribusjonsinfo"}, histogram = true)
	public void settTidLestHoveddokument(JournalpostId journalpostId, OppdaterDistribusjonsInfo oppdaterDistribusjonsinfo) {
		webClient.patch()
				.uri(dokdistdittnavProperties.getDokarkiv().getOppdaterDistribusjonsinfoURI(journalpostId))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
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
				throw new DokarkivOppdaterDistribusjonsinfoFunctionalException(format("Kall mot dokarkiv feilet funksjonelt ved registrering av lest status for journalpost med id=%s, status=%s, feilmelding=%s",
						journalpostId.value(),
						response.getStatusCode(),
						response.getMessage()),
						error);
			} else {
				log.error("Kall mot dokarkiv feilet teknisk ved registrering av lest status for journalpost med id={}, feilmelding={}", journalpostId.value(), error.getMessage());
				throw new DokarkivOppdaterDistribusjonsinfoTechnicalException(format("Kall mot dokarkiv feilet teknisk ved registrering av lest status for journalpost med id=%s ,feilmelding=%s",
						journalpostId.value(),
						error.getMessage()),
						error);
			}
		};
	}
}
