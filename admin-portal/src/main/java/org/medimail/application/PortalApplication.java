package org.medimail.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(
        scanBasePackages = {
                "org.medimail.application",
                "com.medmail.portal"          // controllers & services
        }
)
@EntityScan(basePackages = {
        "com.medmail.portal.domain"       // JPA entities
})
@EnableJpaRepositories(basePackages = {
        "com.medmail.portal.repo"         // Spring Data repositories
})
public class PortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortalApplication.class, args);
    }
}
