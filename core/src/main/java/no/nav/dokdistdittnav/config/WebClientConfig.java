package no.nav.dokdistdittnav.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class WebClientConfig {

	@Bean
	WebClient webClient() {
		var nettyHttpClient = HttpClient.create()
				.responseTimeout(Duration.of(20, ChronoUnit.SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		return WebClient.builder()
				.clientConnector(clientHttpConnector)
				.build();
	}
}
