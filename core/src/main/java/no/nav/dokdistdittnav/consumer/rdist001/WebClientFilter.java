package no.nav.dokdistdittnav.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.ServiceuserAlias;
import no.nav.dokdistdittnav.exception.functional.Rdist001HentForsendelseFunctionalException;
import no.nav.dokdistdittnav.exception.technical.Rdist001HentForsendelseTechnicalException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import static no.nav.dokdistdittnav.constants.MdcConstants.CALL_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;


@Slf4j
public class WebClientFilter implements ExchangeFilterFunction{

	private final ServiceuserAlias serviceuser;

	@Autowired
	public WebClientFilter(ServiceuserAlias serviceuser) {
		this.serviceuser = serviceuser;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return next.exchange(ClientRequest.from(request)
						.headers(httpHeader -> {
							httpHeader.setBasicAuth(serviceuser.getUsername(), serviceuser.getPassword());
							httpHeader.setContentType(APPLICATION_JSON);
							httpHeader.set(CALL_ID, MDC.get(CALL_ID));
						})
				.build());
	}


	public static ExchangeFilterFunction errorHandler() {
		return ExchangeFilterFunction.ofResponseProcessor(response -> {
			if (response.statusCode().is5xxServerError()) {
				return response.bodyToMono(String.class)
						.flatMap(body -> {
							log.error("Feiler med feilmelding: {}", body);
							return Mono.error(new Rdist001HentForsendelseTechnicalException(body));
						});
			} else if (response.statusCode().is4xxClientError()){
				return response.bodyToMono(String.class)
						.flatMap(body -> {
							log.error("Feiler med feilmelding: {}", body);
							return Mono.error(new Rdist001HentForsendelseFunctionalException(body));
						});
			} else {
				return Mono.just(response);
			}
		});
	}

	public static boolean is5xxException(Throwable ex) {
		return ex instanceof Rdist001HentForsendelseTechnicalException;
	}
}
