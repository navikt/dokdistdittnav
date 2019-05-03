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
public class VarselUtsendingQueueCheck extends AbstractDependencyCheck {

	private final Queue varselUtsending;
	private final JmsTemplate jmsTemplate;

	@Inject
	public VarselUtsendingQueueCheck(MeterRegistry registry, Queue varselUtsending, JmsTemplate jmsTemplate) throws JMSException {
		super(DependencyType.QUEUE, "VarselUtsendingQueue", varselUtsending.getQueueName(), Importance.WARNING, registry);
		this.varselUtsending = varselUtsending;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected void doCheck() {
		try {
			checkQueue(varselUtsending);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("JMS Queue Browser failed to get queue: " + varselUtsending, e);
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
