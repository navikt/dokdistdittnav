package no.nav.dokdistdittnav.qdist010;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
		name = "DistribuerForsendelseTilSentralPrint",
		propOrder = {"forsendelseId"}
)
@XmlRootElement(
		namespace = "http://nav.no/melding/virksomhet/dokdistdittnav",
		name = "distribuerForsendelseTilSentralPrint"
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DistribuerForsendelseTilSentralPrint {
	@XmlElement(
			required = true
	)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	@XmlSchemaType(
			name = "forsendelseId"
	)
	protected String forsendelseId;
}
