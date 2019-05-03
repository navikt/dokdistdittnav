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
public class DokumentHenvendelseQueueCheck extends AbstractDependencyCheck {

	private final Queue dokumentHenvendelse;
	private final JmsTemplate jmsTemplate;

	@Inject
	public DokumentHenvendelseQueueCheck(MeterRegistry registry, Queue dokumentHenvendelse, JmsTemplate jmsTemplate) throws JMSException {
		super(DependencyType.QUEUE, "DokumentHenvendelseQueue", dokumentHenvendelse.getQueueName(), Importance.WARNING, registry);
		this.dokumentHenvendelse = dokumentHenvendelse;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected void doCheck() {
		try {
			checkQueue(dokumentHenvendelse);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("JMS Queue Browser failed to get queue: " + dokumentHenvendelse, e);
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
