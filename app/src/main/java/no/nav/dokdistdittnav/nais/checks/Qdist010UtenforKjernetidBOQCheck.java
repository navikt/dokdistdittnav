package no.nav.dokdistdittnav.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistdittnav.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistdittnav.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistdittnav.nais.selftest.DependencyType;
import no.nav.dokdistdittnav.nais.selftest.Importance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Queue;

@Component
public class Qdist010UtenforKjernetidBOQCheck extends AbstractDependencyCheck {

	private final Queue qdist010UtenforKjernetid;
	private final JmsTemplate jmsTemplate;

	@Autowired
	public Qdist010UtenforKjernetidBOQCheck(MeterRegistry registry, Queue qdist010FunksjonellFeil, JmsTemplate jmsTemplate) throws JMSException {
		super(DependencyType.QUEUE, "qdist010UtenforKjernetidQueue", qdist010FunksjonellFeil.getQueueName(), Importance.CRITICAL, registry);
		this.qdist010UtenforKjernetid = qdist010FunksjonellFeil;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected void doCheck() {
		try {
			checkQueue(qdist010UtenforKjernetid);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("JMS Queue Browser failed to get queue: " + qdist010UtenforKjernetid, e);
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
