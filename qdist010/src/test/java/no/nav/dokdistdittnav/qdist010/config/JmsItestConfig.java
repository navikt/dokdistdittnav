package no.nav.dokdistdittnav.qdist010.config;


import com.ibm.mq.jakarta.jms.MQQueue;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;

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
	public Queue qdist010UtenforKjernetid(@Value("${dokdistdittnav_qdist010_dist_ditt_nav_kbq.queuename}") String qdist010UtenforKjernetid) {
		return new ActiveMQQueue(qdist010UtenforKjernetid);
	}

	@Bean
	public Queue qdist009(@Value("${dokdistsentralprint_qdist009_dist_s_print.queuename}") String qdist009QueueName) throws JMSException {
		return new MQQueue(qdist009QueueName);
	}

	@Bean
	public Queue qdist010Bq() {
		return new ActiveMQQueue("qdist010Bq");
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public EmbeddedActiveMQ broker() {
		EmbeddedActiveMQ service = new EmbeddedActiveMQ();
		service.setConfigResourcePath("artemis-server.xml");
		return service;
	}

	@Bean
	@DependsOn("broker")
	public ConnectionFactory activemqConnectionFactory() {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://localhost?create=false");

		JmsPoolConnectionFactory pooledFactory = new JmsPoolConnectionFactory();
		pooledFactory.setConnectionFactory(activeMQConnectionFactory);
		pooledFactory.setMaxConnections(1);
		return pooledFactory;
	}

}

