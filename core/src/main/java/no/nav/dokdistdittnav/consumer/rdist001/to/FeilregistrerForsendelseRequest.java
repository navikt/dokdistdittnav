package no.nav.dokdistdittnav.consumer.rdist001.to;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FeilregistrerForsendelseRequest {
	private Long forsendelseId;
	private String feilTypeCode;
	private String part;
	private LocalDateTime tidspunkt;
	private String detaljer;
	private String resendingDistribusjonId;
}