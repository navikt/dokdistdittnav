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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class LokalCacheConfig {

    public static final String AZURE_CACHE = "azureCache";

    @Bean
    @Primary
    @Profile({"nais", "local"})
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
                new CaffeineCache(AZURE_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(50, TimeUnit.MINUTES)
                        .maximumSize(10)
                        .build())
        ));
        return manager;
    }
}
