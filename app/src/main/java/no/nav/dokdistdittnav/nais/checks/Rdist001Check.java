package no.nav.dokdistdittnav.nais.checks;


import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistdittnav.config.properties.DokdistDittnavServiceuser;
import no.nav.dokdistdittnav.nais.selftest.DependencyType;
import no.nav.dokdistdittnav.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistdittnav.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistdittnav.nais.selftest.Importance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Component
public class Rdist001Check extends AbstractDependencyCheck {

	private final RestTemplate restTemplate;

	@Inject
	public Rdist001Check(MeterRegistry meterRegistry,
						 @Value("${administrerforsendelse.v1.url}") String administrerforsendelseV1Url,
						 RestTemplateBuilder restTemplateBuilder,
						 final DokdistDittnavServiceuser dokdistDittnavServiceuser) {
		super(DependencyType.REST, "rdist001", administrerforsendelseV1Url, Importance.WARNING, meterRegistry);
		this.restTemplate = restTemplateBuilder
				.rootUri(administrerforsendelseV1Url)
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokdistDittnavServiceuser.getUsername(), dokdistDittnavServiceuser.getPassword())
				.build();
	}

	@Override
	protected void doCheck() {
		try {
			restTemplate.getForEntity("/ping", Object.class);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Kunne ikke pinge rdist001", e);
		}
	}

}
