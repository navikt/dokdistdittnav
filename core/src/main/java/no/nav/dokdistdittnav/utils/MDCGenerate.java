package no.nav.dokdistdittnav.utils;

import org.slf4j.MDC;

import java.util.UUID;

import static no.nav.dokdistdittnav.constants.MdcConstants.CALL_ID;

public class MDCGenerate {

	public static void generateNewCallId(String uuid) {
		if (uuid != null) {
			MDC.put(CALL_ID, uuid);
		} else if (MDC.get(CALL_ID) == null) {
			MDC.put(CALL_ID, UUID.randomUUID().toString());
		}
	}

	public static void clearCallId() {
		if (MDC.get(CALL_ID) != null) {
			MDC.remove(CALL_ID);
		}
	}

	private MDCGenerate() {
	}
}
