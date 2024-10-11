
package no.nav.dokdistdittnav.config.jms;

import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.mq.jakarta.jms.MQQueue;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import no.nav.dokdistdittnav.config.properties.DokdistDittnavServiceuser;
import no.nav.dokdistdittnav.config.properties.MqGatewayAlias;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.net.ssl.SSLSocketFactory;

import static com.ibm.mq.constants.CMQC.MQENC_NATIVE;
import static com.ibm.msg.client.jakarta.jms.JmsConstants.JMS_IBM_CHARACTER_SET;
import static com.ibm.msg.client.jakarta.jms.JmsConstants.JMS_IBM_ENCODING;
import static com.ibm.msg.client.jakarta.jms.JmsConstants.USER_AUTHENTICATION_MQCSP;
import static com.ibm.msg.client.jakarta.wmq.common.CommonConstants.WMQ_CM_CLIENT;

@Configuration
@Profile("nais")
public class JmsConfig {

	private static final int UTF_8_WITH_PUA = 1208;
	private static final String ANY_TLS13_OR_HIGHER = "*TLS13ORHIGHER";

	@Bean
	public Queue qdist010(@Value("${dokdistdittnav_qdist010_dist_ditt_nav.queuename}") String qdist010QueueName) throws JMSException {
		return new MQQueue(qdist010QueueName);
	}

	@Bean
	public Queue qdist010FunksjonellFeil(@Value("${dokdistdittnav_qdist010_funk_feil.queuename}") String qdist010FunksjonellFeil) throws JMSException {
		return new MQQueue(qdist010FunksjonellFeil);
	}

	@Bean
	public Queue qdist010UtenforKjernetid(@Value("${dokdistdittnav_qdist010_dist_ditt_nav_kbq.queuename}") String qdist010UtenforKjernetid) throws JMSException {
		return new MQQueue(qdist010UtenforKjernetid);
	}

	@Bean
	public Queue qdist009(@Value("${dokdistsentralprint_qdist009_dist_s_print.queuename}") String qdist009QueueName) throws JMSException {
		return new MQQueue(qdist009QueueName);
	}

	@Bean
	public ConnectionFactory connectionFactory(final MqGatewayAlias mqGatewayAlias,
											   final @Value("${dokdistdittnav_channel_secure.name}") String channelNameSecure,
											   final DokdistDittnavServiceuser dokdistDittnavServiceuser) throws JMSException {
		return createConnectionFactory(mqGatewayAlias, channelNameSecure, dokdistDittnavServiceuser);
	}

	private JmsPoolConnectionFactory createConnectionFactory(final MqGatewayAlias mqGatewayAlias,
															 final String channelNameSecure,
															 final DokdistDittnavServiceuser dokdistDittnavServiceuser) throws JMSException {
		MQConnectionFactory connectionFactory = new MQConnectionFactory();
		connectionFactory.setHostName(mqGatewayAlias.getHostname());
		connectionFactory.setPort(mqGatewayAlias.getPort());
		connectionFactory.setBooleanProperty(USER_AUTHENTICATION_MQCSP, true);

		// https://www.ibm.com/docs/en/ibm-mq/9.1?topic=mm-migrating-existing-security-configurations-use-any-tls12-higher-cipherspec
		connectionFactory.setSSLCipherSuite(ANY_TLS13_OR_HIGHER);
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		connectionFactory.setSSLSocketFactory(factory);
		connectionFactory.setChannel(channelNameSecure);

		connectionFactory.setQueueManager(mqGatewayAlias.getName());
		connectionFactory.setTransportType(WMQ_CM_CLIENT);
		connectionFactory.setCCSID(UTF_8_WITH_PUA);
		connectionFactory.setIntProperty(JMS_IBM_ENCODING, MQENC_NATIVE);
		connectionFactory.setIntProperty(JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA);

		UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
		adapter.setTargetConnectionFactory(connectionFactory);
		adapter.setUsername(dokdistDittnavServiceuser.getUsername());
		adapter.setPassword(dokdistDittnavServiceuser.getPassword());

		JmsPoolConnectionFactory pooledFactory = new JmsPoolConnectionFactory();
		pooledFactory.setConnectionFactory(adapter);
		pooledFactory.setMaxConnections(10);
		pooledFactory.setMaxSessionsPerConnection(10);
		return pooledFactory;
	}
}
