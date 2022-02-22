package no.nav.dokdistdittnav.consumer.rdist001;

import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
import no.nav.dokdistdittnav.constants.MdcConstants;
import no.nav.dokdistdittnav.constants.RetryConstants;
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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;
import java.time.Duration;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final String administrerforsendelseV1Url;
	private final RestTemplate restTemplate;

	@Inject
	public AdministrerForsendelseConsumer(@Value("${administrerforsendelse.v1.url}") String administrerforsendelseV1Url,
										  RestTemplateBuilder restTemplateBuilder,
										  final ServiceuserAlias serviceuserAlias) {
		this.administrerforsendelseV1Url = administrerforsendelseV1Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = RetryConstants.DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	@Monitor(value = "dok_consumer", extraTags = {"process", "hentForsendelse"}, histogram = true)
	public HentForsendelseResponseTo hentForsendelse(final String forsendelseId) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders());
			HentForsendelseResponseTo forsendelse =
					restTemplate.exchange(this.administrerforsendelseV1Url + "/" + forsendelseId, HttpMethod.GET, entity, HentForsendelseResponseTo.class)
							.getBody();

			if (forsendelse.getArkivInformasjon() == null) {
				throw new Rdist001HentForsendelseFunctionalException("Kall mot rdist001 - hentForsendelse returnerte forsendelse uten ArkivInformasjon");
			}

			return forsendelse;
		} catch (HttpClientErrorException e) {
			throw new Rdist001HentForsendelseFunctionalException(String.format("Kall mot rdist001 - hentForsendelse feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new Rdist001HentForsendelseTechnicalException(String.format("Kall mot rdist001 - hentForsendelse feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = RetryConstants.DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	@Monitor(value = "dok_consumer", extraTags = {"process", "oppdaterForsendelseStatus"}, histogram = true)
	public void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus, String varselStatus) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders());
			String uri = UriComponentsBuilder.fromHttpUrl(administrerforsendelseV1Url)
					.queryParam("forsendelseId", forsendelseId)
					.queryParam("forsendelseStatus", forsendelseStatus)
					.queryParam("varselStatus", varselStatus)
					.toUriString();
			restTemplate.exchange(uri, HttpMethod.PUT, entity, Object.class);
		} catch (HttpClientErrorException e) {
			throw new Rdist001OppdaterForsendelseStatusFunctionalException(String.format("Kall mot rdist001 - oppdaterForsendelseStatus feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new Rdist001OppdaterForsendelseStatusTechnicalException(String.format("Kall mot rdist001 - oppdaterForsendelseStatus feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(MdcConstants.CALL_ID, MDC.get(MdcConstants.CALL_ID));
		return headers;
	}

}
