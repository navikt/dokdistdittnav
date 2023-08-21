package no.nav.dokdistdittnav.config;

import lombok.extern.slf4j.Slf4j;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jakarta.jms.ConnectionFactory;

/**
 * Rydder opp ressurser som Spring ikke gjør selv.
 */
@Slf4j
@Component
public class ShutdownHook {

	@Autowired
	private ConnectionFactory connectionFactory;

	@PreDestroy
	public void destroy() {
		log.info("Graceful shutdown - Lukker koblinger til ConnectionFactory pool");
		((JmsPoolConnectionFactory) connectionFactory).clear();
	}
}
