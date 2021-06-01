package no.nav.dokdistdittnav;

import no.nav.dokdistdittnav.config.alias.MqGatewayAlias;
import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableConfigurationProperties({ServiceuserAlias.class,
        MqGatewayAlias.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
