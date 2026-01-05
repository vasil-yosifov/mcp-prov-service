package com.telecom.ocs.provisioning.controllers;

import com.telecom.ocs.provisioning.api.model.Usage;
import com.telecom.ocs.provisioning.exceptions.DuplicateResourceException;
import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.mappers.UsageMapper;
import com.telecom.ocs.provisioning.services.UsageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for usage record operations.
 * Implements endpoints for creating and listing usage records.
 */
@RestController
public class UsageController {

    private static final Logger logger = LoggerFactory.getLogger(UsageController.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_OFFSET = 0;

    private final UsageService usageService;
    private final UsageMapper usageMapper;

    public UsageController(UsageService usageService, UsageMapper usageMapper) {
        this.usageService = usageService;
        this.usageMapper = usageMapper;
    }

    /**
     * Create a usage record for a subscriber.
     * 
     * @param usage The usage record to create
     * @param xTransactionID Transaction ID for request tracking
     * @return The created usage record with HTTP 201 status
     * @throws ResourceNotFoundException if subscriber or balance not found (404)
     * @throws DuplicateResourceException if usage ID already exists (409)
     */
    @PostMapping(value = "/usage", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Usage> usagePost(
            @Valid @RequestBody Usage usage,
            @RequestHeader(value = "X-Transaction-ID", required = false) String xTransactionID) {
        logger.info("Creating usage record with ID: {}, transactionID: {}", 
            usage.getUsageId(), xTransactionID);
        
        try {
            // Convert DTO to entity
            com.telecom.ocs.provisioning.models.Usage usageEntity = usageMapper.toEntity(usage);
            
            // Create usage record (includes balance update)
            com.telecom.ocs.provisioning.models.Usage createdEntity = 
                usageService.createUsage(usageEntity);
            
            // Convert entity back to DTO
            Usage createdDto = usageMapper.toDto(createdEntity);
            
            logger.info("Successfully created usage record with ID: {}", createdDto.getUsageId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
            
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found while creating usage: {}", e.getMessage());
            throw e;
        } catch (DuplicateResourceException e) {
            logger.error("Duplicate usage ID: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating usage record: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * List usage records for a subscriber with pagination support.
     * 
     * @param subscriberId The subscriber ID to fetch usage records for
     * @param limit Maximum number of items to return (1-100)
     * @param offset Offset for pagination (default: 0)
     * @param xTransactionID Transaction ID for request tracking
     * @return List of usage records with HTTP 200 status
     * @throws ResourceNotFoundException if subscriber not found (404)
     */
    @GetMapping(value = "/subscribers/{subscriberId}/usage", produces = "application/json")
    public ResponseEntity<List<Usage>> subscribersSubscriberIdUsageGet(
            @PathVariable("subscriberId") String subscriberId,
            @Min(1) @Max(100) @RequestParam(value = "limit", required = false) Integer limit,
            @Min(0) @RequestParam(value = "offset", required = false) Integer offset,
            @RequestHeader(value = "X-Transaction-ID", required = false) String xTransactionID) {
        
        logger.info("Fetching usage records for subscriberId: {}, limit: {}, offset: {}, transactionID: {}",
            subscriberId, limit, offset, xTransactionID);
        
        try {
            // Set defaults if not provided
            int pageSize = (limit != null) ? limit : DEFAULT_PAGE_SIZE;
            int pageOffset = (offset != null) ? offset : DEFAULT_PAGE_OFFSET;
            
            // Create pageable request
            Pageable pageable = PageRequest.of(pageOffset / pageSize, pageSize);
            
            // Fetch usage records
            Page<com.telecom.ocs.provisioning.models.Usage> usagePage = 
                usageService.listUsageBySubscriberId(subscriberId, pageable);
            
            // Convert entities to DTOs
            List<Usage> usageDtos = usagePage.getContent().stream()
                .map(usageMapper::toDto)
                .collect(Collectors.toList());
            
            logger.info("Successfully fetched {} usage records for subscriberId: {}", 
                usageDtos.size(), subscriberId);
            
            return ResponseEntity.ok(usageDtos);
            
        } catch (ResourceNotFoundException e) {
            logger.error("Subscriber not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error fetching usage records: {}", e.getMessage(), e);
            throw e;
        }
    }
}
