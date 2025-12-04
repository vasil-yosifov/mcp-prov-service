package com.telecom.ocs.provisioning.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Filter to extract or generate X-Transaction-ID header for request tracing.
 * 
 * If the X-Transaction-ID header is present, it will be used as the transaction ID.
 * If absent, a transaction ID will be generated using the format: {endpoint}_{unixTimestamp}
 * 
 * The transaction ID is stored in MDC for logging purposes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionIdFilter extends OncePerRequestFilter {

    public static final String TRANSACTION_ID_HEADER = "X-Transaction-ID";
    public static final String MDC_TRANSACTION_ID_KEY = "transactionId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String transactionId = request.getHeader(TRANSACTION_ID_HEADER);
            
            if (transactionId == null || transactionId.isBlank()) {
                // Generate transaction ID: endpoint_unixTimestamp
                String endpoint = sanitizeEndpoint(request.getRequestURI());
                long unixTimestamp = Instant.now().toEpochMilli();
                transactionId = endpoint + "_" + unixTimestamp;
            }
            
            // Store in MDC for logging
            MDC.put(MDC_TRANSACTION_ID_KEY, transactionId);
            
            // Add to response header for client correlation
            response.setHeader(TRANSACTION_ID_HEADER, transactionId);
            
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.remove(MDC_TRANSACTION_ID_KEY);
        }
    }

    /**
     * Sanitizes the endpoint path for use in transaction ID.
     * Removes leading slash and replaces special characters with underscores.
     * 
     * @param uri the request URI
     * @return sanitized endpoint string
     */
    private String sanitizeEndpoint(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "unknown";
        }
        
        // Remove leading slash and context path prefix if present
        String endpoint = uri.replaceFirst("^/ocs/prov/v1/", "")
                            .replaceFirst("^/", "");
        
        // Replace slashes and special characters with underscores
        endpoint = endpoint.replaceAll("[/\\-{}]", "_")
                          .replaceAll("_+", "_")  // Collapse multiple underscores
                          .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
        
        // Truncate if too long
        if (endpoint.length() > 50) {
            endpoint = endpoint.substring(0, 50);
        }
        
        return endpoint.isEmpty() ? "root" : endpoint;
    }
}
