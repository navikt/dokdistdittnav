package no.nav.dokdistdittnav;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@ComponentScan
@Configuration
public class CoreConfig {

	@Bean
	public Clock clock(){
		return Clock.systemDefaultZone();
	}
}
