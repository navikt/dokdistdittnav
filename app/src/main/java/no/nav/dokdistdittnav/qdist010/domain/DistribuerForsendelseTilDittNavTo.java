package no.nav.dokdistdittnav.qdist010.domain;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */

@Value
@Builder
public class DistribuerForsendelseTilDittNavTo {

	private String forsendelseId;
}
