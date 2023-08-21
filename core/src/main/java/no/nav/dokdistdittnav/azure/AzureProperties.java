package no.nav.dokdistdittnav.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * Konfigurert av naiserator. https://doc.nais.io/security/auth/azure-ad/#runtime-variables-credentials
 */
@Validated
@ConfigurationProperties(prefix = "azure.app")
public record AzureProperties(
		@NotEmpty String tokenUrl,
		@NotEmpty String clientId,
		@NotEmpty String clientSecret,
		@NotEmpty String tenantId,
		@NotEmpty String wellKnownUrl
) {
	public static final String SPRING_DEFAULT_PRINCIPAL = "anonymousUser";
	public static final String CLIENT_REGISTRATION_DOKNOTIFIKASJON = "azure-doknotifikasjon";
	public static final String CLIENT_REGISTRATION_DOKARKIV = "azure-dokarkiv";
	public static final String CLIENT_REGISTRATION_DOKDISTADMIN = "azure-dokdistadmin";

	public static OAuth2AuthorizeRequest getOAuth2AuthorizeRequestForAzure(String clientRegistrationId) {
		return OAuth2AuthorizeRequest
				.withClientRegistrationId(clientRegistrationId)
				.principal(SPRING_DEFAULT_PRINCIPAL)
				.build();
	}
}
