package no.nav.dokdistdittnav.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import reactor.netty.transport.ProxyProvider;

@Slf4j
@ConfigurationProperties(prefix = "http")
public record WebProxyProperties(
		String proxyHost,
		int proxyPort,
		String nonProxyHosts
) {

	public <T extends ProxyProvider.TypeSpec> void setProxy(T proxyProvider) {
		log.info("Setter proxy proxyHost={}. proxyPort={}", proxyHost, proxyPort);
		if (proxyHost != null) {
			proxyProvider.type(ProxyProvider.Proxy.HTTP).host(proxyHost).port(proxyPort).nonProxyHosts("localhost|127.0.0.1|10.254.0.1|.+.local|.+.adeo.no|.+.nav.no|.+.aetat.no|.+.devillo.no|.+.oera.no|.+nais.io|.+.aivencloud.com");
		}
	}
}
