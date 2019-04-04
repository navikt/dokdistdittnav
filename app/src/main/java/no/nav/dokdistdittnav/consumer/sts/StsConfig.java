package no.nav.dokdistdittnav.consumer.sts;

import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
import org.apache.cxf.Bus;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
public class StsConfig {
	private static final String STS_CLIENT_AUTHENTICATION_POLICY = "classpath:policy/untPolicy.xml";

	@Bean
	STSClient stsClient(@Value("${securityTokenService.url}") String stsUrl,
						final ServiceuserAlias serviceuserAlias,
						Bus cxf) {
		STSClient stsClient = new STSClient(cxf);
		stsClient.setEnableAppliesTo(false);
		stsClient.setAllowRenewing(false);
		stsClient.setLocation(stsUrl);
		stsClient.setTokenType("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");
		stsClient.setKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer");

		HashMap<String, Object> properties = new HashMap<>();
		properties.put(SecurityConstants.USERNAME, serviceuserAlias.getUsername());
		properties.put(SecurityConstants.PASSWORD, serviceuserAlias.getPassword());

		stsClient.setProperties(properties);

		//used for the STS client to authenticate itself to the STS provider.
		stsClient.setPolicy(STS_CLIENT_AUTHENTICATION_POLICY);
		return stsClient;
	}
}
