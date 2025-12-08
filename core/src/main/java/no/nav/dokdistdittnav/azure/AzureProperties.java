package no.nav.dokdistdittnav.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * Konfigurert av naiserator. https://doc.nais.io/security/auth/azure-ad/#runtime-variables-credentials
 */
@Validated
@ConfigurationProperties(prefix = "azure")
public record AzureProperties(
		@NotEmpty String openidConfigTokenEndpoint,
		@NotEmpty String appClientId,
		@NotEmpty String appClientSecret
) {
	public static final String CLIENT_REGISTRATION_DOKNOTIFIKASJON = "azure-doknotifikasjon";
	public static final String CLIENT_REGISTRATION_DOKARKIV = "azure-dokarkiv";
	public static final String CLIENT_REGISTRATION_DOKDISTADMIN = "azure-dokdistadmin";
}