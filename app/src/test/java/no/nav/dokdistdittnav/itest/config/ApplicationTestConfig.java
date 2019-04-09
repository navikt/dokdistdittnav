package no.nav.dokdistdittnav.itest.config;

import static org.mockito.Mockito.mock;

import no.nav.dokdistdittnav.storage.Storage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
@Import(JmsItestConfig.class)
public class ApplicationTestConfig {

	@Bean
	public Storage storage() {
		return mock(Storage.class);
	}

}

