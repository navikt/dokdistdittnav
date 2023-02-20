package no.nav.dokdistdittnav.utils;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import static no.nav.dokdistdittnav.constants.NavHeaders.NAV_CALLID;
import static no.nav.dokdistdittnav.utils.MDCUtils.getCallId;

public class NavHeadersFilter implements ExchangeFilterFunction {
	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

		return next.exchange(ClientRequest.from(request).headers((headers) -> headers.set(NAV_CALLID, getCallId())).build());
	}
}