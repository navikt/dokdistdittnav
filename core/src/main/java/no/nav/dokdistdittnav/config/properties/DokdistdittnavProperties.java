package no.nav.dokdistdittnav.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Data
@Validated
@ConfigurationProperties(prefix = "dokdistdittnav")
public class DokdistdittnavProperties {

	private boolean autostartup;
	private String appnavn;

	private final Topic topic = new Topic();
	private final MinSide minside = new MinSide();
	private final Doknotifikasjon doknotifikasjon = new Doknotifikasjon();
	private final Dokarkiv dokarkiv = new Dokarkiv();
	private final Dokdist dokdistadmin = new Dokdist();

	@Data
	@Validated
	public static class Topic {
		@NotNull
		private String lestavmottaker;
	}

	@Data
	@Validated
	public static class MinSide {
		@NotEmpty
		private String varseltopic;
		@NotEmpty
		private String dokumentarkivLink;
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
	}

	@Data
	@Validated
	public static class Dokarkiv {
		@NotEmpty
		private String baseUri;
		@NotEmpty
		private String oauthScope;
	}
}
