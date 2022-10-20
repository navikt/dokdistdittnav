package no.nav.dokdistdittnav.consumer.azure;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.AzureTokenProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.function.Consumer;

import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.dokdistdittnav.config.cache.LokalCacheConfig.AZURE_CACHE;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Slf4j
@Component
@Profile({"nais", "local"})
public class AzureTokenConsumer {

	private static final String AZURE_TOKEN_INSTANCE = "azuretoken";
	private final WebClient webClient;
	private final AzureTokenProperties azureTokenProperties;

	public AzureTokenConsumer(AzureTokenProperties azureTokenProperties) {
		this.azureTokenProperties = azureTokenProperties;
		webClient = createWebclient(azureTokenProperties);

	}

	@Retry(name = AZURE_TOKEN_INSTANCE)
	@CircuitBreaker(name = AZURE_TOKEN_INSTANCE)
	@Cacheable(AZURE_CACHE)
	public TokenResponse getClientCredentialToken(String scope) {
		String form = "grant_type=client_credentials&scope=" + scope + "&client_id=" +
				azureTokenProperties.clientId() + "&client_secret=" + azureTokenProperties.clientSecret();
		return webClient.post()
				.bodyValue(form)
				.retrieve()
				.bodyToMono(TokenResponse.class)
				.doOnError(handleError())
				.block();
	}


	private Consumer<Throwable> handleError() {
		return error -> {
			throw new AzureTokenException(String.format("Klarte ikke hente token fra Azure. Feilet med httpstatus=%s. Feilmelding=%s", ((WebClientResponseException) error).getStatusCode(), error.getMessage()), error);
		};
	}

	private WebClient createWebclient(AzureTokenProperties azureTokenProperties) {
		return WebClient.builder()
				.baseUrl(azureTokenProperties.tokenUrl())
				.defaultHeaders(getHeaders())
				.clientConnector(new ReactorClientHttpConnector(createHttpClient()))
				.build();
	}

	private HttpClient createHttpClient(){
		return HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.of(20, SECONDS));
	}

	private Consumer<HttpHeaders> getHeaders() {
		return headers -> {
			headers.set(CONTENT_TYPE, String.valueOf(APPLICATION_FORM_URLENCODED));
			headers.set(ACCEPT, String.valueOf(MediaType.APPLICATION_JSON));
		};
	}


}
