package no.nav.dokdistdittnav.utils;

import org.slf4j.MDC;

import java.util.UUID;

import static no.nav.dokdistdittnav.constants.MdcConstants.MDC_CALL_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class MDCUtils {

	private MDCUtils() {
	}

	public static String getCallId() {
		if (isBlank(MDC.get(MDC_CALL_ID))) {
			generateNewCallId();
		}
		return MDC.get(MDC_CALL_ID);
	}

	public static void generateNewCallId() {
		MDC.put(MDC_CALL_ID, UUID.randomUUID().toString());
	}
}
