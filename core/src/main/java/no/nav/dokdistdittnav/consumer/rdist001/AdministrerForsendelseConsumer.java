package no.nav.dokdistdittnav.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.OppdaterVarselInfoRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FeilregistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.OppdaterForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.DokdistadminFunctionalException;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.exception.technical.DokdistadminTechnicalException;
import no.nav.dokdistdittnav.utils.NavHeadersFilter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.util.Objects.nonNull;
import static no.nav.dokdistdittnav.azure.AzureProperties.CLIENT_REGISTRATION_DOKDISTADMIN;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final WebClient webClient;

	public AdministrerForsendelseConsumer(DokdistdittnavProperties dokdistdittnavProperties,
										  WebClient webClient) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistdittnavProperties.getDokdistadmin().getBaseUri())
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Override
	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public HentForsendelseResponse hentForsendelse(final String forsendelseId) {
		log.info("hentForsendelse henter forsendelse med forsendelseId={}", forsendelseId);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{forsendelseId}")
						.build(forsendelseId))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.retrieve()
				.bodyToMono(HentForsendelseResponse.class)
				.onErrorMap(this::mapError)
				.block();

		log.info("hentForsendelse har hentet forsendelse med forsendelseId={}", forsendelseId);
		return response;
	}

	@Override
	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public String finnForsendelse(final FinnForsendelseRequest finnForsendelseRequest) {
		var oppslagsnoekkel = finnForsendelseRequest.getOppslagsnoekkel().noekkel;
		var verdi = finnForsendelseRequest.getVerdi();

		log.info("finnForsendelse henter forsendelse med {}={}", oppslagsnoekkel, verdi);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/finnforsendelse/{oppslagsnoekkel}/{verdi}")
						.build(oppslagsnoekkel, verdi))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.retrieve()
				.bodyToMono(FinnForsendelseResponse.class)
				.map(FinnForsendelseResponse::getForsendelseId)
				.onErrorMap(this::mapError)
				.block();

		log.info("finnForsendelse har hentet forsendelse med forsendelseId={} og {}={}", response, oppslagsnoekkel, verdi);
		return response;
	}

	@Override
	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public OpprettForsendelseResponse opprettForsendelse(final OpprettForsendelseRequest opprettForsendelseRequest) {
		log.info("opprettForsendelse oppretter forsendelse med bestillingsId={}", opprettForsendelseRequest.getBestillingsId());

		var response = webClient.post()
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(opprettForsendelseRequest)
				.retrieve()
				.bodyToMono(OpprettForsendelseResponse.class)
				.onErrorMap(this::mapError)
				.block();

		var forsendelseId = response != null ? response.getForsendelseId() : null;
		log.info("opprettForsendelse har opprettet forsendelse med forsendelseId={} og bestillingsId={}",
				forsendelseId, opprettForsendelseRequest.getBestillingsId());
		return response;
	}

	@Override
	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse) {
		log.info("feilregistrerForsendelse feilregistrerer forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());

		webClient.put()
				.uri(uriBuilder -> uriBuilder.path("/feilregistrerforsendelse").build())
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(feilregistrerForsendelse)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();

		log.info("feilregistrerForsendelse har feilregistrert forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());
	}

	@Override
	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelse) {
		log.info("oppdaterForsendelse oppdaterer forsendelse med forsendelseId={}", oppdaterForsendelse.forsendelseId());

		webClient.put()
				.uri("/oppdaterforsendelse")
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(oppdaterForsendelse)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();

		oppdaterForsendelseLogg(oppdaterForsendelse);
	}

	@Override
	@Retryable(retryFor = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfo) {
		log.info("oppdaterVarselInfo oppdaterer varselinfo for forsendelse med forsendelseId={}", oppdaterVarselInfo.forsendelseId());

		webClient.put()
				.uri(uriBuilder -> uriBuilder.path("/oppdatervarselinfo").build())
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(oppdaterVarselInfo)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();

		log.info("oppdaterVarselInfo har oppdatert varselinfo for forsendelse med forsendelseId={}", oppdaterVarselInfo.forsendelseId());
	}

	private void oppdaterForsendelseLogg(OppdaterForsendelseRequest oppdaterForsendelse) {
		if (nonNull(oppdaterForsendelse.varselStatus()) && isNotBlank(oppdaterForsendelse.forsendelseStatus())) {
			log.info("oppdaterForsendelse oppdatert forsendelse med forsendelseId={} til forsendelseStatus={} og varselStatus={} ", oppdaterForsendelse.forsendelseId(),
					oppdaterForsendelse.forsendelseStatus(), oppdaterForsendelse.varselStatus());
		} else if (isNotBlank(oppdaterForsendelse.forsendelseStatus())) {
			log.info("oppdaterForsendelse oppdatert forsendelse med forsendelseId={} til forsendelseStatus={}", oppdaterForsendelse.forsendelseId(), oppdaterForsendelse.forsendelseStatus());
		} else {
			log.info("oppdaterForsendelse oppdatert forsendelse med forsendelseId={} til varselStatus={}", oppdaterForsendelse.forsendelseId(), oppdaterForsendelse.varselStatus());
		}
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			return new DokdistadminFunctionalException(
					String.format("Kall mot rdist001 feilet funksjonelt med status=%s, feilmelding=%s",
							response.getStatusCode(),
							response.getMessage()),
					error);
		} else {
			return new DokdistadminTechnicalException(
					String.format("Kall mot rdist001 feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}
}
