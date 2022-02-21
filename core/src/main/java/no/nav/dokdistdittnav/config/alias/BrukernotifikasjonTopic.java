package no.nav.dokdistdittnav.config.alias;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@Validated
@ConfigurationProperties(prefix = "brukernotifikasjon")
public class BrukernotifikasjonTopic {
	@NotNull
	private String topicbeskjed;
	@NotNull
	private String topicoppgave;
	@NotNull
	private String url;
}
