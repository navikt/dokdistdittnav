package no.nav.dokdistdittnav.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.azure.AzureProperties;
import no.nav.dokdistdittnav.config.properties.DokdistDittnavServiceuser;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.constants.RetryConstants;
import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.OppdaterVarselInfoRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FeilregistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.OppdaterForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.DokdistadminFunctionalException;
import no.nav.dokdistdittnav.exception.functional.Rdist001OppdaterForsendelseStatusFunctionalException;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.exception.technical.DokdistadminTechnicalException;
import no.nav.dokdistdittnav.exception.technical.Rdist001OppdaterForsendelseStatusTechnicalException;
import no.nav.dokdistdittnav.utils.NavHeadersFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static no.nav.dokdistdittnav.constants.MdcConstants.MDC_CALL_ID;
import static no.nav.dokdistdittnav.constants.NavHeaders.NAV_CALLID;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final RestTemplate restTemplate;
	private final String administrerforsendelseV1Url;
	private final WebClient webClient;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	@Autowired
	public AdministrerForsendelseConsumer(RestTemplateBuilder restTemplateBuilder,
										  final DokdistDittnavServiceuser dittnavServiceuser,
										  @Value("${administrerforsendelse.v1.url}") String administrerforsendelseV1Url,
										  DokdistdittnavProperties dokdistdittnavProperties,
										  WebClient webClient,
										  ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this.administrerforsendelseV1Url = administrerforsendelseV1Url;
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dittnavServiceuser.getUsername(), dittnavServiceuser.getPassword())
				.build();
		this.webClient = webClient.mutate()
				.baseUrl(dokdistdittnavProperties.getDokdistadmin().getBaseUri())
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	public HentForsendelseResponse hentForsendelse(final String forsendelseId) {

		log.info("hentForsendelse henter forsendelse med forsendelseId={}", forsendelseId);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{forsendelseId}")
						.build(forsendelseId))
				.attributes(getOAuth2AuthorizedClient())
				.retrieve()
				.bodyToMono(HentForsendelseResponse.class)
				.doOnError(this::handleError)
				.block();

		log.info("hentForsendelse har hentet forsendelse med forsendelseId={}", forsendelseId);

		return response;
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	public FinnForsendelseResponseTo finnForsendelse(final FinnForsendelseRequestTo finnForsendelseRequestTo) {
		String uri = UriComponentsBuilder.fromHttpUrl(administrerforsendelseV1Url)
				.path("/finnforsendelse")
				.queryParam(finnForsendelseRequestTo.getOppslagsNoekkel(), finnForsendelseRequestTo.getVerdi())
				.toUriString();
		try {
			HttpEntity<?> entity = new HttpEntity<>(createHeaders());
			log.info("Mottatt kall for å finne forsendelse med {}={}", finnForsendelseRequestTo.getOppslagsNoekkel(), finnForsendelseRequestTo.getVerdi());
			ResponseEntity<FinnForsendelseResponseTo> response = restTemplate.exchange(uri, GET, entity, FinnForsendelseResponseTo.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			throw new Rdist001OppdaterForsendelseStatusFunctionalException(format("Kall mot rdist001 - finnForsendelse feilet med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()),
					e);

		} catch (HttpServerErrorException e) {
			throw new Rdist001OppdaterForsendelseStatusTechnicalException(format("Kall mot rdist001 - finnForsendelse feilet teknisk med statusCode=%s,feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public OpprettForsendelseResponse opprettForsendelse(final OpprettForsendelseRequest opprettForsendelseRequest) {
		log.info("opprettForsendelse oppretter forsendelse med bestillingsId={}", opprettForsendelseRequest.getBestillingsId());

		var response = webClient.post()
				.attributes(getOAuth2AuthorizedClient())
				.bodyValue(opprettForsendelseRequest)
				.retrieve()
				.bodyToMono(OpprettForsendelseResponse.class)
				.doOnError(this::handleError)
				.block();

		var forsendelseId = response != null ? response.getForsendelseId() : null;
		log.info("opprettForsendelse har opprettet forsendelse med forsendelseId={} og bestillingsId={}",
				forsendelseId, opprettForsendelseRequest.getBestillingsId());

		return response;
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse) {
		log.info("feilregistrerForsendelse feilregistrerer forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());

		webClient.put()
				.uri(uriBuilder -> uriBuilder.path("/feilregistrerforsendelse").build())
				.attributes(getOAuth2AuthorizedClient())
				.bodyValue(feilregistrerForsendelse)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("feilregistrerForsendelse har feilregistrert forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelse) {
		log.info("oppdaterForsendelse oppdaterer forsendelse med forsendelseId={}", oppdaterForsendelse.forsendelseId());

		webClient.put()
				.uri("/oppdaterforsendelse")
				.attributes(getOAuth2AuthorizedClient())
				.bodyValue(oppdaterForsendelse)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		oppdaterForsendelseLogg(oppdaterForsendelse);
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfo) {
		log.info("oppdaterVarselInfo oppdaterer varselinfo for forsendelse med forsendelseId={}", oppdaterVarselInfo.forsendelseId());

		webClient.put()
				.uri(uriBuilder -> uriBuilder.path("/oppdatervarselinfo").build())
				.attributes(getOAuth2AuthorizedClient())
				.bodyValue(oppdaterVarselInfo)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("oppdaterVarselInfo har oppdatert varselinfo for forsendelse med forsendelseId={}", oppdaterVarselInfo.forsendelseId());
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(NAV_CALLID, MDC.get(MDC_CALL_ID));
		return headers;
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

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new DokdistadminFunctionalException(
					String.format("Kall mot rdist001 feilet funksjonelt med status=%s, feilmelding=%s",
							response.getRawStatusCode(),
							response.getMessage()),
					error);
		} else {
			throw new DokdistadminTechnicalException(
					String.format("Kall mot rdist001 feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(AzureProperties.getOAuth2AuthorizeRequestForAzure(AzureProperties.CLIENT_REGISTRATION_DOKDISTADMIN));
		return ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(clientMono.block());
	}

}
