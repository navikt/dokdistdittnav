package no.nav.dokdistdittnav.qdist010.util;

import static java.lang.String.format;

import no.nav.dokdistdittnav.constants.DomainConstants;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.functional.InvalidForsendelseStatusException;
import no.nav.dokdistdittnav.storage.DokdistDokument;
import no.nav.dokdistdittnav.qdist010.domain.BestillingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public final class Qdist009FunctionalUtils {
	private Qdist009FunctionalUtils() {
	}

	public static void validateForsendelseStatus(String forsendelseStatus) {
		if (!DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DIST.equals(forsendelseStatus)) {
			throw new InvalidForsendelseStatusException(String.format("ForsendelseStatus må være %s. Fant forsendelseStatus=%s", DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DIST, forsendelseStatus));
		}
	}

	public static String getDokumenttypeIdHoveddokument(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.filter(dokumentTo -> DomainConstants.HOVEDDOKUMENT.equals(dokumentTo.getTilknyttetSom()))
				.map(HentForsendelseResponseTo.DokumentTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}

	public static List<BestillingEntity> createBestillingEntities(String bestillingId, String bestillingXml, List<DokdistDokument> dokdistDokumentList) {
		List<BestillingEntity> bestillingEntities = new ArrayList<>();
		BestillingEntity bestillingXmlEntity = createBestillingXmlEntity(bestillingId, bestillingXml);
		List<BestillingEntity> documentEntities = createDocumentEntities(dokdistDokumentList);
		bestillingEntities.add(bestillingXmlEntity);
		bestillingEntities.addAll(documentEntities);
		return bestillingEntities;
	}

	private static BestillingEntity createBestillingXmlEntity(String bestillingId, String bestillingXml) {
		return BestillingEntity.builder()
				.fileName(format("%s.xml", bestillingId))
				.entity(bestillingXml.getBytes())
				.build();
	}

	private static List<BestillingEntity> createDocumentEntities(List<DokdistDokument> dokdistDokumentList) {
		return dokdistDokumentList.stream()
				.map(dokdistDokument -> (BestillingEntity.builder()
						.fileName(format("%s.pdf", dokdistDokument.getDokumentObjektReferanse()))
						.entity(dokdistDokument.getPdf())
						.build()))
				.collect(Collectors.toList());
	}


}
