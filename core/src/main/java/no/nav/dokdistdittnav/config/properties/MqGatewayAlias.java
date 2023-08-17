package no.nav.dokdistdittnav.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@ConfigurationProperties("mqgateway01")
@Validated
public class MqGatewayAlias {
	@NotEmpty
	private String hostname;
	@NotEmpty
	private String name;
	@Min(0)
	private int port;
	private boolean enableTls;
}
