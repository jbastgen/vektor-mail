package de.vektor.mail.osgi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.osgi.framework.BundleContext;

@Configuration
public class OsgiConfiguration {

    @Bean
    public BundleContext bundleContext(FelixOsgiFramework felixOsgiFramework) {
        return felixOsgiFramework.getBundleContext();
    }
}
