package no.nav.dokdistdittnav.config.alias;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "brukernotifikasjon")
public class BrukernotifikasjonTopic {
	private String topicbeskjed;
	private String topicoppgave;
}
