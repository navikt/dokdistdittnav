package no.nav.dokdistdittnav.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import reactor.netty.transport.ProxyProvider;

@Slf4j
@ConfigurationProperties("https")
public record WebProxyProperties(String proxyHost, int proxyPort) {

	public <T extends ProxyProvider.TypeSpec> void setProxy(T proxyProvider) {
		log.info("Setter proxy proxyHost={}. proxyPort={}", proxyHost, proxyPort);
		if (proxyHost != null) {
			proxyProvider.type(ProxyProvider.Proxy.HTTP).host(proxyHost).port(proxyPort);
		}
	}
}
