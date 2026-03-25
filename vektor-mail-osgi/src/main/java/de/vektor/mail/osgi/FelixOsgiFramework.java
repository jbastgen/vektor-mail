package de.vektor.mail.osgi;

import org.apache.felix.framework.Felix;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FelixOsgiFramework implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(FelixOsgiFramework.class);

    private Framework framework;

    @Override
    public void afterPropertiesSet() throws BundleException {
        log.info("Starting Apache Felix OSGi Framework...");

        Map<String, String> config = new HashMap<>();
        config.put("felix.cache.profiledir", "felix-cache");
        config.put("felix.cache.dir", "felix-cache");
        config.put("org.osgi.framework.storage", "felix-cache");
        config.put("org.osgi.framework.storage.clean", "onFirstInit");

        framework = new Felix(config);
        framework.start();

        log.info("Apache Felix OSGi Framework started. State: {}", framework.getState());
    }

    @Override
    public void destroy() throws BundleException, InterruptedException {
        if (framework != null) {
            log.info("Stopping Apache Felix OSGi Framework...");
            framework.stop();
            framework.waitForStop(5000);
            log.info("Apache Felix OSGi Framework stopped.");
        }
    }

    public BundleContext getBundleContext() {
        return framework != null ? framework.getBundleContext() : null;
    }

    public Framework getFramework() {
        return framework;
    }
}
