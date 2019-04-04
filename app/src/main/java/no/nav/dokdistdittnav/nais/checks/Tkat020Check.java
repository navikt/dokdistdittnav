package no.nav.dokdistdittnav.nais.checks;


import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
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
public class Tkat020Check extends AbstractDependencyCheck {

	private final RestTemplate restTemplate;

	@Inject
	public Tkat020Check(MeterRegistry meterRegistry,
						@Value("${DokumenttypeInfo_v4_url}") String dokumenttypeInfoV4Url,
						RestTemplateBuilder restTemplateBuilder,
						final ServiceuserAlias serviceuserAlias) {
		super(DependencyType.REST, "tkat020", dokumenttypeInfoV4Url, Importance.WARNING, meterRegistry);
		this.restTemplate = restTemplateBuilder
				.rootUri(dokumenttypeInfoV4Url)
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Override
	protected void doCheck() {
		try {
			restTemplate.getForEntity("/ping", String.class);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Kunne ikke pinge tkat020", e);
		}
	}

}
