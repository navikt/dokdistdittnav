package no.nav.dokdistdittnav.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistdittnav.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistdittnav.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistdittnav.nais.selftest.DependencyType;
import no.nav.dokdistdittnav.nais.selftest.Importance;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Queue;

@Component
public class Qdist009FunksjonellBOQCheck extends AbstractDependencyCheck {

	private final Queue qdist009FunksjonellFeil;
	private final JmsTemplate jmsTemplate;

	@Inject
	public Qdist009FunksjonellBOQCheck(MeterRegistry registry, Queue qdist009FunksjonellFeil, JmsTemplate jmsTemplate) throws JMSException {
		super(DependencyType.QUEUE, "qdist008FunksjonellFeilQueue", qdist009FunksjonellFeil.getQueueName(), Importance.CRITICAL, registry);
		this.qdist009FunksjonellFeil = qdist009FunksjonellFeil;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected void doCheck() {
		try {
			checkQueue(qdist009FunksjonellFeil);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("JMS Queue Browser failed to get queue: " + qdist009FunksjonellFeil, e);
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
