package no.nav.dokdistdittnav.config.properties;

import lombok.Data;
import no.nav.dokdistdittnav.consumer.dokarkiv.JournalpostId;
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
	private final Dokarkiv dokarkiv = new Dokarkiv();
	private final Dokdist dokdist = new Dokdist();
	private final Dokdist dokdistadmin = new Dokdist();

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
		@NotEmpty
		private String baseUri;
		@NotEmpty
		private String oauthScope;

		public URI getNotifikasjonInfoURI(String bestillingsId) {
			return URI.create(baseUri + bestillingsId);
		}

	}

	@Data
	@Validated
	public static class Dokdist {
		@NotEmpty
		private String baseUri;
		@NotEmpty
		private String oauthScope;

		public URI getDokdistURI() {
			return URI.create(baseUri);
		}

	}

	@Data
	@Validated
	public static class Dokarkiv {
		private final String oppdaterDistribusjonsinfoPath = "/oppdaterDistribusjonsinfo";
		@NotEmpty
		private String baseUri;
		@NotEmpty
		private String oauthScope;

		public URI getOppdaterDistribusjonsinfoURI(JournalpostId journalPostId) {
			return URI.create(baseUri + "/" + journalPostId.value() + oppdaterDistribusjonsinfoPath);
		}
	}
}
