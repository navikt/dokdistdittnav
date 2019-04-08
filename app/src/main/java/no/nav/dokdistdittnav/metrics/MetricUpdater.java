package no.nav.dokdistdittnav.metrics;

import static no.nav.dokdistdittnav.qdist010.Qdist010Route.SERVICE_ID;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
//todo bruk meg eller kast meg!
public class MetricUpdater {

	private static final String QDIST010_SERVICE = "dok_business_counter";
	private static final String LABEL_LANDKODE = "landkode";
	private static final String LABEL_POSTDESTINASJON = "postdestinasjon";

	private final MeterRegistry meterRegistry;

	public MetricUpdater(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void updateQdist009Metrics(String landkode,
											 String postdestinasjon) {
		meterRegistry.counter(QDIST010_SERVICE,
				MetricLabels.LABEL_PROCESS, SERVICE_ID,
				LABEL_LANDKODE, landkode,
				LABEL_POSTDESTINASJON, postdestinasjon).increment();
	}

}
