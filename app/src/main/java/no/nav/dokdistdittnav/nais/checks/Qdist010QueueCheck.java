package no.nav.dokdistdittnav.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistdittnav.nais.selftest.DependencyType;
import no.nav.dokdistdittnav.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistdittnav.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistdittnav.nais.selftest.Importance;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.JMSException;
import javax.jms.Queue;

@Component
public class Qdist010QueueCheck extends AbstractDependencyCheck {

	private final Queue qdist010;
	private final JmsTemplate jmsTemplate;

	@Autowired
	public Qdist010QueueCheck(MeterRegistry registry, Queue qdist010, JmsTemplate jmsTemplate) throws JMSException {
		super(DependencyType.QUEUE, "Qdist010Queue", qdist010.getQueueName(), Importance.CRITICAL, registry);
		this.qdist010 = qdist010;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected void doCheck() {
		try {
			checkQueue(qdist010);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("JMS Queue Browser failed to get queue: " + qdist010, e);
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
