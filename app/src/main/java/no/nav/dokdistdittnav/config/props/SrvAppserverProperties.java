package no.nav.dokdistdittnav.config.props;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;

/**
 * Workaround for the special srvappserver user found on Jboss. Used to authenticate with the WMQ channel
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
@Getter
@Setter
@ToString
@ConfigurationProperties("srvappserver")
@Validated
public class SrvAppserverProperties {
	@NotEmpty
	private String username;
	private String password;

	@PostConstruct
	public void postConstruct() {
		if (***passord=gammelt_passord***) {
			password = "";
		}
	}
}
