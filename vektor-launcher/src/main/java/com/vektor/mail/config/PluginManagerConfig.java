package com.vektor.mail.config;

import org.pf4j.spring.SpringPluginManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Configures the PF4J SpringPluginManager.
 * Plugins are loaded from the configured directory at startup.
 */
@Configuration
public class PluginManagerConfig {

    @Value("${vektor.plugins.dir:./plugins}")
    private String pluginsDir;

    @Bean
    public SpringPluginManager pluginManager() {
        SpringPluginManager manager = new SpringPluginManager(Path.of(pluginsDir));
        manager.loadPlugins();
        manager.startPlugins();
        return manager;
    }
}
