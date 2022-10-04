
package no.nav.dokdistdittnav.config.jms;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import no.nav.dokdistdittnav.config.properties.MqGatewayAlias;
import no.nav.dokdistdittnav.config.properties.DokdistDittnavServiceuser;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.net.ssl.SSLSocketFactory;

import java.util.concurrent.TimeUnit;

import static com.ibm.mq.constants.CMQC.MQENC_NATIVE;
import static com.ibm.msg.client.jms.JmsConstants.JMS_IBM_CHARACTER_SET;
import static com.ibm.msg.client.jms.JmsConstants.JMS_IBM_ENCODING;
import static com.ibm.msg.client.jms.JmsConstants.USER_AUTHENTICATION_MQCSP;
import static com.ibm.msg.client.wmq.common.CommonConstants.WMQ_CM_CLIENT;

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
											   final @Value("${dokdistdittnav_channel.name}") String channelName,
											   final @Value("${dokdistdittnav_channel_secure.name}") String channelNameSecure,
											   final DokdistDittnavServiceuser dokdistDittnavServiceuser) throws JMSException {
		return createConnectionFactory(mqGatewayAlias, channelName, channelNameSecure, dokdistDittnavServiceuser);
	}

	private PooledConnectionFactory createConnectionFactory(final MqGatewayAlias mqGatewayAlias,
															final String channelName,
															final String channelNameSecure,
															final DokdistDittnavServiceuser dokdistDittnavServiceuser) throws JMSException {
		MQConnectionFactory connectionFactory = new MQConnectionFactory();
		connectionFactory.setHostName(mqGatewayAlias.getHostname());
		connectionFactory.setPort(mqGatewayAlias.getPort());
		connectionFactory.setBooleanProperty(USER_AUTHENTICATION_MQCSP, true);

		if (mqGatewayAlias.isEnableTls()) {
			// https://www.ibm.com/docs/en/ibm-mq/9.1?topic=mm-migrating-existing-security-configurations-use-any-tls12-higher-cipherspec
			connectionFactory.setSSLCipherSuite(ANY_TLS13_OR_HIGHER);
			SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			connectionFactory.setSSLSocketFactory(factory);
			connectionFactory.setChannel(channelNameSecure);
		} else {
			connectionFactory.setChannel(channelName);
		}

		connectionFactory.setQueueManager(mqGatewayAlias.getName());
		connectionFactory.setTransportType(WMQ_CM_CLIENT);
		connectionFactory.setCCSID(UTF_8_WITH_PUA);
		connectionFactory.setIntProperty(JMS_IBM_ENCODING, MQENC_NATIVE);
		connectionFactory.setIntProperty(JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA);

		UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
		adapter.setTargetConnectionFactory(connectionFactory);
		adapter.setUsername(dokdistDittnavServiceuser.getUsername());
		adapter.setPassword(dokdistDittnavServiceuser.getPassword());

		PooledConnectionFactory pooledFactory = new PooledConnectionFactory();
		pooledFactory.setConnectionFactory(adapter);
		pooledFactory.setMaxConnections(10);
		pooledFactory.setMaximumActiveSessionPerConnection(10);
		pooledFactory.setReconnectOnException(true);
		pooledFactory.setExpiryTimeout(TimeUnit.HOURS.toMillis(24));
		return pooledFactory;
	}
}
