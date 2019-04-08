package no.nav.dokdistdittnav.consumer.dokkat.tkat021;

import static java.lang.String.format;

import javax.inject.Inject;
import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
import no.nav.dokdistdittnav.config.cache.LokalCacheConfig;
import no.nav.dokdistdittnav.constants.RetryConstants;
import no.nav.dokdistdittnav.exception.functional.Tkat021FunctionalException;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.exception.technical.Tkat021TechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import no.nav.dokkat.schemas.tkat021.VarselInfoRestTo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
@Component
public class VarselInfoConsumer implements VarselInfo {

	private final String varselInfoV1Url;
	private final RestTemplate restTemplate;

	@Inject
	public VarselInfoConsumer(@Value("${DokumenttypeInfo_v4_url}") String varselInfoV1Url,
							  RestTemplateBuilder restTemplateBuilder, final
							  ServiceuserAlias serviceuserAlias) {
		this.varselInfoV1Url = varselInfoV1Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}


	@Override
	@Cacheable(LokalCacheConfig.TKAT021_CACHE)
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = RetryConstants.DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	@Monitor(value = "dok_consumer", extraTags = {"process", "getVarselInfo"}, histogram = true)
	public VarselInfoTo getVarselInfo(String varselTypeId) {
		try {
			VarselInfoRestTo response = restTemplate.getForObject(this.varselInfoV1Url + "/" + varselTypeId,
					VarselInfoRestTo.class);
			return mapResponse(response);
		} catch (HttpClientErrorException e) {
			throw new Tkat021FunctionalException(format(
					"TKAT021 feilet med statusKode=%s. Fant ingen VarselInfo med VarselTypeId=%s. Feilmelding=%s",
					e.getStatusCode(),
					varselTypeId,
					e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new Tkat021TechnicalException(format("TKAT021 feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}

	private VarselInfoTo mapResponse(final VarselInfoRestTo response) {
		if (response == null) {
			throw new Tkat021FunctionalException(format("VarselInfo er null"));
		}
		return VarselInfoTo.builder()
				.varselTypeId(response.getVarseltypeId())
				.repeterendeVarsel(response.getAntallRevarslinger() == null ? false:true)
				.build();
	}
}
