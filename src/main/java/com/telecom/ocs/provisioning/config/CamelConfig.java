package com.telecom.ocs.provisioning.config;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apache Camel configuration for OCS Provisioning Service.
 * Configures Camel context and provides integration routing capabilities.
 * Camel routes can be used for future integration patterns such as:
 * - Timer-based batch processing
 * - External system integration
 * - Event-driven workflows
 * - Message transformation and routing
 */
@Configuration
public class CamelConfig {

    private static final Logger logger = LoggerFactory.getLogger(CamelConfig.class);

    /**
     * Configures the Camel context with custom settings.
     * This provides a foundation for future Camel routes while keeping
     * the core REST API functionality in Spring MVC controllers.
     */
    @Bean
    public CamelContextConfiguration camelContextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                logger.info("Configuring Camel context for OCS Provisioning Service");
                
                // Enable MDC logging for correlation IDs in Camel routes
                camelContext.setUseMDCLogging(true);
                
                // Set stream caching for handling large messages if needed
                camelContext.getStreamCachingStrategy().setEnabled(true);
                
                logger.info("Camel context configuration completed");
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                logger.info("Camel context started successfully with {} routes", 
                    camelContext.getRoutes().size());
            }
        };
    }
}
