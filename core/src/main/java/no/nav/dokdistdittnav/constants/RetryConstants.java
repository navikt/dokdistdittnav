package no.nav.dokdistdittnav.constants;

public final class RetryConstants {

	private RetryConstants() {
	}

	//retries: 2s, 10s, 50s, 4.2min, 20.8min, sum = max vente-tid = 26min. maxAttempts inkluderer første forsøk
	public static final int DELAY_LONG = 2000;

	//retries: 500ms, 1s sum = max vente-tid = 1.5s. maxAttempts inkluderer første forsøk
	public static final int MAX_ATTEMPTS_SHORT = 3;
	public static final int DELAY_SHORT = 500;
	public static final int MULTIPLIER_SHORT = 2;

}
