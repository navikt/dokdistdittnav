package no.nav.dokdistdittnav.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistDittnavServiceuser;
import no.nav.dokdistdittnav.constants.RetryConstants;
import no.nav.dokdistdittnav.consumer.rdist001.to.FeilRegistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.exception.functional.Rdist001HentForsendelseFunctionalException;
import no.nav.dokdistdittnav.exception.functional.Rdist001OppdaterForsendelseStatusFunctionalException;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.exception.technical.Rdist001HentForsendelseTechnicalException;
import no.nav.dokdistdittnav.exception.technical.Rdist001OppdaterForsendelseStatusTechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static javax.servlet.RequestDispatcher.ERROR_MESSAGE;
import static no.nav.dokdistdittnav.constants.DomainConstants.FORSENDELSE_ID;
import static no.nav.dokdistdittnav.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistdittnav.constants.MdcConstants.DOK_CONSUMER;
import static no.nav.dokdistdittnav.constants.MdcConstants.PROCESS;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_MESSAGE;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Slf4j
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final String administrerforsendelseV1Url;
	private final RestTemplate restTemplate;

	@Inject
	public AdministrerForsendelseConsumer(@Value("${administrerforsendelse.v1.url}") String administrerforsendelseV1Url,
										  RestTemplateBuilder restTemplateBuilder,
										  final DokdistDittnavServiceuser dittnavServiceuser) {
		this.administrerforsendelseV1Url = administrerforsendelseV1Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dittnavServiceuser.getUsername(), dittnavServiceuser.getPassword())
				.build();
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	@Monitor(value = DOK_CONSUMER, extraTags = {PROCESS, "hentForsendelse"}, histogram = true)
	public HentForsendelseResponseTo hentForsendelse(final String forsendelseId) {
		try {
			HttpEntity<?> entity = new HttpEntity<>(createHeaders());
			HentForsendelseResponseTo forsendelse = restTemplate.exchange(this.administrerforsendelseV1Url + "/" + forsendelseId, HttpMethod.GET, entity, HentForsendelseResponseTo.class)
							.getBody();

			if (isNull(forsendelse) && isNull(forsendelse.getArkivInformasjon())) {
				throw new Rdist001HentForsendelseFunctionalException("Kall mot rdist001 - hentForsendelse returnerte forsendelse uten ArkivInformasjon");
			}

			return forsendelse;
		} catch (HttpClientErrorException e) {
			throw new Rdist001HentForsendelseFunctionalException(format("Kall mot rdist001 - hentForsendelse feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new Rdist001HentForsendelseTechnicalException(format("Kall mot rdist001 - hentForsendelse feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	@Monitor(value = DOK_CONSUMER, extraTags = {PROCESS, "finnForsendelse"}, histogram = true)
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
	@Monitor(value = DOK_CONSUMER, extraTags = {PROCESS, "persisterForsendelse"}, histogram = true)
	public PersisterForsendelseResponseTo persisterForsendelse(final PersisterForsendelseRequestTo forsendelseRequestTo) {
		try {
			HttpEntity<?> entity = new HttpEntity<>(forsendelseRequestTo, createHeaders());
			ResponseEntity<PersisterForsendelseResponseTo> response = restTemplate.exchange(administrerforsendelseV1Url, POST, entity, PersisterForsendelseResponseTo.class);
			return response.getBody();

		} catch (HttpClientErrorException e) {
			throw new Rdist001HentForsendelseFunctionalException(String.format("Kall mot rdist001 - feilet til å opprette forsendelse med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new Rdist001HentForsendelseTechnicalException(String.format("Kall mot rdist001 - feilet til å opprette forsendelse med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	@Monitor(value = DOK_CONSUMER, extraTags = {PROCESS, "feilRegistrerForsendelse"}, histogram = true)
	public void feilRegistrerForsendelse(FeilRegistrerForsendelseRequest feilRegistrerForsendelse) {

		try {
			HttpEntity<?> entity = new HttpEntity<>(feilRegistrerForsendelse, createHeaders());
			restTemplate.exchange(administrerforsendelseV1Url + "/feilregistrerforsendelse", PUT, entity, Object.class);
		} catch (HttpClientErrorException e) {
			log.error("Kall mot rdist001 - feilet til å feilregistrer forsendelse med forsendelseId={}, feilmelding={}", feilRegistrerForsendelse.getForsendelseId(), e.getMessage());
			throw new AbstractDokdistdittnavFunctionalException(format("Kall mot rdist001 - feilet til å opprette forsendelse med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e) {
			};
		} catch (HttpServerErrorException e) {
			log.error("Kall mot rdist001 - feilet til å feilregistrer forsendelse med forsendelseId={}, feilmelding={}", feilRegistrerForsendelse.getForsendelseId(), e.getMessage());
			throw new AbstractDokdistdittnavTechnicalException(format("Kall mot rdist001 - feilet til å opprette forsendelse med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e) {
			};
		}
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	@Monitor(value = DOK_CONSUMER, extraTags = {PROCESS, "oppdaterForsendelseStatus"}, histogram = true)
	public void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus) {
		String uri = UriComponentsBuilder.fromHttpUrl(administrerforsendelseV1Url)
				.queryParam(FORSENDELSE_ID, forsendelseId)
				.queryParam("forsendelseStatus", forsendelseStatus)
				.toUriString();
		log.info("Mottatt kall til å oppdatere forsendelse med forsendelseId={} til forsendelseStatus={}", forsendelseId, forsendelseStatus);
		oppdaterForsendelse(uri);

	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	@Monitor(value = DOK_CONSUMER, extraTags = {PROCESS, "oppdaterKonversasjonsId"}, histogram = true)
	public void oppdaterForsendelseAndVarselStatus(String forsendelseId, String forsendelseStatus, String varselStatus) {
		String uri = UriComponentsBuilder.fromHttpUrl(administrerforsendelseV1Url)
				.queryParam(FORSENDELSE_ID, forsendelseId)
				.queryParam("forsendelseStatus", forsendelseStatus)
				.queryParam("varselStatus", varselStatus)
				.toUriString();
		log.info("Mottatt kall til å oppdatere forsendelse med forsendelseId={} til forsendelseStatus={} og varselStatus={}", forsendelseId, forsendelseStatus, varselStatus);
		oppdaterForsendelse(uri);

	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	@Monitor(value = DOK_CONSUMER, extraTags = {PROCESS, "oppdaterVarselStatus"}, histogram = true)
	public void oppdaterVarselStatus(String forsendelseId, String varselStatus) {
		String uri = UriComponentsBuilder.fromHttpUrl(administrerforsendelseV1Url)
				.queryParam(FORSENDELSE_ID, forsendelseId)
				.queryParam("varselStatus", varselStatus)
				.toUriString();
		log.info("Mottatt kall til å oppdatere forsendelse med forsendelseId={} til varselStatus={}", forsendelseId, varselStatus);
		oppdaterForsendelse(uri);

	}

	private void oppdaterForsendelse(String uri) {
		try {
			HttpEntity<?> entity = new HttpEntity<>(createHeaders());
			restTemplate.exchange(uri, PUT, entity, String.class);
		} catch (HttpClientErrorException e) {
			throw new Rdist001HentForsendelseFunctionalException(format("Kall mot rdist001 - oppdaterForsendelse feilet med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()),
					e);

		} catch (HttpServerErrorException e) {
			throw new Rdist001HentForsendelseTechnicalException(format("Kall mot rdist001 - oppdaterForsendelse feilet teknisk med statusCode=%s,feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(CALL_ID, MDC.get(CALL_ID));
		return headers;
	}

}
