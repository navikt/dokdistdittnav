package no.nav.dokdistdittnav.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@Validated
@ConfigurationProperties(prefix = "dokdistdittnav")
public class DokdistdittnavProperties {

	private String appnavn;
	private final Topic topic = new Topic();
	private final Brukernotifikasjon brukernotifikasjon = new Brukernotifikasjon();
	private final Doknotifikasjon doknotifikasjon = new Doknotifikasjon();

	@Data
	@Validated
	public static class Topic {
		@NotNull
		private String lestavmottaker;
	}

	@Data
	@Validated
	public static class Brukernotifikasjon {
		@NotNull
		private String topicbeskjed;
		@NotNull
		private String topicoppgave;
		@NotNull
		private String topicdone;
		@NotNull
		private String link;
	}

	@Data
	@Validated
	public static class Doknotifikasjon {
		@NotNull
		private String status;
	}
}
