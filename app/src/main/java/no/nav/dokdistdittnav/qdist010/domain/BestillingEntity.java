package no.nav.dokdistdittnav.qdist010.domain;

import static java.lang.String.format;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class BestillingEntity {
	private final String fileName;
	private final byte[] entity;

	public String toString() {
		return format("BestillingEntity{filnavn=%s, entity.length=%s}", this.fileName, this.entity == null ? null : this.entity.length);
	}
}
