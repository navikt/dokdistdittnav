package no.nav.dokdistdittnav.consumer.dokkat.tkat020;

import static java.lang.String.format;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
import no.nav.dokdistdittnav.config.cache.LokalCacheConfig;
import no.nav.dokdistdittnav.constants.DomainConstants;
import no.nav.dokdistdittnav.constants.RetryConstants;
import no.nav.dokdistdittnav.consumer.dokkat.tkat021.VarselInfoConsumer;
import no.nav.dokdistdittnav.exception.technical.Tkat020TechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import no.nav.dokdistdittnav.exception.functional.Tkat020FunctionalException;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokkat.api.tkat020.v4.DokumentTypeInfoToV4;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

import java.time.Duration;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */
@Slf4j
@Component
class DokumentkatalogAdminConsumer implements DokumentkatalogAdmin {

	private final String dokumenttypeInfoV4Url;
	private final RestTemplate restTemplate;
	private VarselInfoConsumer varselInfoConsumer;

	@Inject
	public DokumentkatalogAdminConsumer(@Value("${DokumenttypeInfo_v4_url}") String dokumenttypeInfoV4Url,
										RestTemplateBuilder restTemplateBuilder,
										final ServiceuserAlias serviceuserAlias) {
		this.dokumenttypeInfoV4Url = dokumenttypeInfoV4Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Override
	@Cacheable(LokalCacheConfig.TKAT020_CACHE)
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = RetryConstants.DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	@Monitor(value = "dok_consumer", extraTags = {"process", "getDokumenttypeInfo"}, histogram = true)
	public DokumenttypeInfoTo getDokumenttypeInfo(final String dokumenttypeId) {
		try {
			DokumentTypeInfoToV4 response = restTemplate.getForObject(this.dokumenttypeInfoV4Url + "/" + dokumenttypeId,
					DokumentTypeInfoToV4.class);
			return mapResponse(response);
		} catch (HttpClientErrorException e) {
			throw new Tkat020FunctionalException(format(
					"TKAT020 feilet med statusKode=%s. Fant ingen dokumenttypeInfo med dokumenttypeId=%s. Feilmelding=%s",
					e
							.getStatusCode(),
					dokumenttypeId,
					e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new Tkat020TechnicalException(format("TKAT020 feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}

	private DokumenttypeInfoTo mapResponse(final DokumentTypeInfoToV4 response) {
		if (response.getDokumentProduksjonsInfo() == null || response.getDokumentProduksjonsInfo()
				.getDistribusjonInfo() == null) {
			throw new Tkat020FunctionalException(format(
					"dokkat.DokumentProduksjonsInfo eller dokkat.DokumentProduksjonsInfo.DistribusjonInfo er null på dokument med dokumenttypeId=%s. Ikke et utgående dokument? dokumentType=%s",
					response
							.getDokumenttypeId(),
					response.getDokumentType()));
		}
		return DokumenttypeInfoTo.builder()
				.dokumenttypeId(response.getDokumenttypeId())
				.dokumentTittel(response.getDokumentTittel())
				.vedlegg(response.getDokumentProduksjonsInfo().getVedlegg())
				.ikkeRedigerbarMalId(response.getDokumentProduksjonsInfo().getIkkeRedigerbarMalId())
				.redigerbarMalId(response.getDokumentProduksjonsInfo().getRedigerbarMalId())
				.malLogikkFil(response.getDokumentProduksjonsInfo().getMalLogikkFil())
				.malXsdReferanse(response.getDokumentProduksjonsInfo().getMalXsdReferanse())
				.dokumentKategori(response.getDokumentKategori())
				.sensitivt(response.getSensitivt())
				.varselTypeId(response.getDokumentProduksjonsInfo()
						.getDistribusjonInfo()
						.getDistribusjonVarsels()
						.stream()
						.filter(distribusjonVarselTo -> DomainConstants.DISTRIBUSJONS_KANAL.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))
						.findAny()
						.get()
						.getVarseltypeId())
				.build();
	}

}
