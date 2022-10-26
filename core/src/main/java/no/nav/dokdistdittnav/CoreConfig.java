package no.nav.dokdistdittnav;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
public class CoreConfig {

	@Bean
	public Clock clock(){
		return Clock.systemDefaultZone();
	}
}
