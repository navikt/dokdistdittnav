package no.nav.dokdistdittnav.consumer.dokkat.tkat021;

import lombok.Builder;
import lombok.Value;

/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
@Value
@Builder
public class VarselInfoTo {

	private final String varselTypeId;
	private final boolean repeterendeVarsel;
}
