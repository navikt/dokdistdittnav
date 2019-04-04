package no.nav.dokdistdittnav.consumer.sts;

import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.ws.security.trust.STSClient;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Slf4j
@Component
public class STSTokenRetriever {
	private final STSClient stsClient;

	@Inject
	public STSTokenRetriever(STSClient stsClient) {
		this.stsClient = stsClient;
	}

	public String requestSecurityToken() throws Exception {
		return elementToString(stsClient.requestSecurityToken().getToken());
	}

	private String elementToString(Element xmlElement) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StringWriter stringWriter = new StringWriter();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(new DOMSource(xmlElement), new StreamResult(stringWriter));
			return stringWriter.toString();
		} catch (TransformerException e) {
			throw new IllegalStateException("Could not parse element to String " + xmlElement, e);
		}
	}
}
