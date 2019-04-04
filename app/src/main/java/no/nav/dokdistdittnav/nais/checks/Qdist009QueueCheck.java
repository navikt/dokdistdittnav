package no.nav.dokdistdittnav.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistdittnav.nais.selftest.DependencyType;
import no.nav.dokdistdittnav.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistdittnav.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistdittnav.nais.selftest.Importance;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Queue;

@Component
public class Qdist009QueueCheck extends AbstractDependencyCheck {

	private final Queue qdist009;
	private final JmsTemplate jmsTemplate;

	@Inject
	public Qdist009QueueCheck(MeterRegistry registry, Queue qdist009, JmsTemplate jmsTemplate) throws JMSException {
		super(DependencyType.QUEUE, "Qdist008Queue", qdist009.getQueueName(), Importance.CRITICAL, registry);
		this.qdist009 = qdist009;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected void doCheck() {
		try {
			checkQueue(qdist009);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("JMS Queue Browser failed to get queue: " + qdist009, e);
		}
	}

	private void checkQueue(final Queue queue) {
		jmsTemplate.browse(queue,
				(session, browser) -> {
					browser.getQueue();
					return null;
				}
		);
	}


}
