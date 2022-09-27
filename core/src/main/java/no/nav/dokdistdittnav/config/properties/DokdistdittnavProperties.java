package no.nav.dokdistdittnav.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.URI;

@Data
@Validated
@ConfigurationProperties(prefix = "dokdistdittnav")
public class DokdistdittnavProperties {

	private boolean autostartup;
	private String appnavn;
	private final Topic topic = new Topic();
	private final Brukernotifikasjon brukernotifikasjon = new Brukernotifikasjon();
	private final Doknotifikasjon doknotifikasjon = new Doknotifikasjon();
	private final DokArkiv dokArkiv = new DokArkiv();

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
		private String statustopic;
	}

	@Data
	@Validated
	public static class DokArkiv {
		@NotEmpty
		private String oauthScope;
		@NotEmpty
		private String oppdaterDistribusjonsinfoUri;

		public URI getOppdaterDistribusjonsinfoUri() {
			return URI.create(oppdaterDistribusjonsinfoUri);
		}
	}
}
