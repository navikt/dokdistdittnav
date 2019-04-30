package no.nav.dokdistdittnav.itest.config;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
public class JmsItestConfig {

	@Bean
	public Queue qdist010(@Value("${dokdistdittnav_qdist010_dist_ditt_nav.queuename}") String qdist010QueueName) {
		return new ActiveMQQueue(qdist010QueueName);
	}

	@Bean
	public Queue qdist010FunksjonellFeil(@Value("${dokdistdittnav_qdist010_funk_feil.queuename}") String qdist010FunksjonellFeil) {
		return new ActiveMQQueue(qdist010FunksjonellFeil);
	}

	@Bean
	public Queue backoutQueue() {
		return new ActiveMQQueue("ActiveMQ.DLQ");
	}

	@Bean
	public Queue dokumentHenvendelse(@Value("${henvendelse_dokument.henvendelse.queuename}") String henvendelseDokumentQueueName) {
		return new ActiveMQQueue(henvendelseDokumentQueueName);
	}

	@Bean
	public Queue varselUtsending(@Value("${varselproduksjon.best_varsel_m_handling.queuename}") String varselUtsendingQueueName) {
		return new ActiveMQQueue(varselUtsendingQueueName);
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public BrokerService broker() {
		BrokerService service = new BrokerService();
		service.setPersistent(false);
		return service;
	}

	@Bean
	public ConnectionFactory activemqConnectionFactory() {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://localhost?create=false");
		RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
		redeliveryPolicy.setMaximumRedeliveries(0);
		activeMQConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);
		return activeMQConnectionFactory;
	}
}

