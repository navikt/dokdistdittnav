package no.nav.dokdistdittnav.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

@Validated
@ConfigurationProperties(prefix = "azure.app")
public record AzureTokenProperties(
		@NotEmpty String tokenUrl,
		@NotEmpty String clientId,
		@NotEmpty String clientSecret,
		@NotEmpty String tenantId,
		@NotEmpty String wellKnownUrl
) {
	public static final Authentication ANONYMOUS_AUTHENTICATION =  new AnonymousAuthenticationToken(
			"anonymous", "anonymousUser", createAuthorityList("ROLE_ANONYMOUS"));
	public static final String CLIENT_REGISTRATION_ID = "azure";

	public static OAuth2AuthorizeRequest getOAuth2AuthorizeRequestForAzure() {
		return OAuth2AuthorizeRequest
				.withClientRegistrationId(CLIENT_REGISTRATION_ID)
				.principal(ANONYMOUS_AUTHENTICATION)
				.build();
	}
}
