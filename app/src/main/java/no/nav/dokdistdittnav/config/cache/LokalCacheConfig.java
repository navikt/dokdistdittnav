package no.nav.dokdistdittnav.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@EnableCaching
public class LokalCacheConfig {

	public static final String TKAT020_CACHE = "tkat020Cache";

	@Bean
	@Primary
	@Profile({"nais", "local"})
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Collections.singletonList(
				new CaffeineCache(TKAT020_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(1, TimeUnit.DAYS)
						.build())
		));
		return manager;
	}
}
