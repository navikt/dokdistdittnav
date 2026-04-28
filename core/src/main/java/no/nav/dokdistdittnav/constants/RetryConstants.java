package no.nav.dokdistdittnav.constants;

public final class RetryConstants {

	private RetryConstants() {
	}

	//retries: 2s, 10s, 50s, 4.2min, 20.8min, sum = max vente-tid = 26min
	public static final int DELAY_LONG = 2000;

	public static final int DELAY_SHORT = 500;
	public static final int MULTIPLIER_SHORT = 2;

}
