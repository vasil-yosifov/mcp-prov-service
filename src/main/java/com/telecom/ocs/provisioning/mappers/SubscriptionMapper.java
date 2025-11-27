package com.telecom.ocs.provisioning.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.ocs.provisioning.models.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for converting between Subscription JPA entity and OpenAPI generated DTOs.
 * 
 * Handles:
 * - State enum mapping (entity uppercase to DTO lowercase)
 * - CycleLengthType enum to String conversion
 * - LocalDateTime to OffsetDateTime conversion
 * - JSON String to Map conversion for customParameters
 * 
 * Supports task T066 (SubscriptionMapper for entity ↔ DTO conversion)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionMapper {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Vienna");
    private final ObjectMapper objectMapper;

    /**
     * Convert Subscription entity to OpenAPI DTO
     * 
     * @param entity the JPA entity
     * @return the OpenAPI DTO
     */
    public com.telecom.ocs.provisioning.api.model.Subscription toDto(Subscription entity) {
        if (entity == null) {
            return null;
        }

        com.telecom.ocs.provisioning.api.model.Subscription dto = 
            new com.telecom.ocs.provisioning.api.model.Subscription();

        // Basic fields
        dto.setSubscriptionId(entity.getSubscriptionId());
        dto.setSubscriberId(entity.getSubscriberId());
        dto.setSubscriptionType(entity.getSubscriptionType());
        dto.setOfferId(entity.getOfferId());
        dto.setOfferName(entity.getOfferName());

        // State mapping (entity enum to DTO enum)
        dto.setState(mapStateToDto(entity.getState()));

        // Date mappings (LocalDateTime to OffsetDateTime)
        dto.setCreationDate(toOffsetDateTime(entity.getCreationDate()));
        dto.setActivationDate(toOffsetDateTime(entity.getActivationDate()));
        dto.setExpirationDate(toOffsetDateTime(entity.getExpirationDate()));
        dto.setRenewalDate(toOffsetDateTime(entity.getRenewalDate()));
        dto.setLastModifiedDate(toOffsetDateTime(entity.getLastModifiedDate()));

        // Recurring cycle fields
        dto.setRecurring(entity.getRecurring());
        dto.setPaidFlag(entity.getPaidFlag());
        dto.setIsGroup(entity.getIsGroup());
        dto.setMaxRecurringCycles(entity.getMaxRecurringCycles());
        dto.setRecurringCyclesCompleted(entity.getRecurringCyclesCompleted());
        dto.setCycleLengthUnits(entity.getCycleLengthUnits());
        
        // CycleLengthType enum to String
        dto.setCycleLengthType(mapCycleLengthTypeToDto(entity.getCycleLengthType()));

        // Custom parameters (JSON String to Map)
        dto.setCustomParameters(parseCustomParameters(entity.getCustomParameters()));

        return dto;
    }

    /**
     * Convert OpenAPI DTO to Subscription entity
     * 
     * @param dto the OpenAPI DTO
     * @return the JPA entity
     */
    public Subscription toEntity(com.telecom.ocs.provisioning.api.model.Subscription dto) {
        if (dto == null) {
            return null;
        }

        Subscription entity = Subscription.builder()
                .subscriberId(dto.getSubscriberId())
                .subscriptionType(dto.getSubscriptionType())
                .offerId(dto.getOfferId())
                .offerName(dto.getOfferName())
                .recurring(dto.getRecurring() != null ? dto.getRecurring() : false)
                .paidFlag(dto.getPaidFlag() != null ? dto.getPaidFlag() : false)
                .isGroup(dto.getIsGroup() != null ? dto.getIsGroup() : false)
                .maxRecurringCycles(dto.getMaxRecurringCycles())
                .recurringCyclesCompleted(dto.getRecurringCyclesCompleted() != null ? dto.getRecurringCyclesCompleted() : 0)
                .cycleLengthUnits(dto.getCycleLengthUnits())
                .build();

        // State mapping (DTO enum to entity enum)
        entity.setState(mapStateToEntity(dto.getState()));

        // CycleLengthType String to enum
        entity.setCycleLengthType(mapCycleLengthTypeToEntity(dto.getCycleLengthType()));

        // Date mappings (OffsetDateTime to LocalDateTime)
        entity.setActivationDate(toLocalDateTime(dto.getActivationDate()));
        entity.setExpirationDate(toLocalDateTime(dto.getExpirationDate()));
        entity.setRenewalDate(toLocalDateTime(dto.getRenewalDate()));

        // Custom parameters (Map to JSON String)
        entity.setCustomParameters(serializeCustomParameters(dto.getCustomParameters()));

        return entity;
    }

    /**
     * Update existing entity with values from DTO
     * Used for PATCH operations where we want to preserve existing values
     * 
     * @param existing the existing entity to update
     * @param dto the DTO with new values
     * @return the updated entity
     */
    public Subscription updateEntityFromDto(Subscription existing, 
            com.telecom.ocs.provisioning.api.model.Subscription dto) {
        if (dto == null) {
            return existing;
        }

        // Only update non-null fields from DTO
        if (dto.getSubscriptionType() != null) {
            existing.setSubscriptionType(dto.getSubscriptionType());
        }
        if (dto.getOfferName() != null) {
            existing.setOfferName(dto.getOfferName());
        }
        if (dto.getRecurring() != null) {
            existing.setRecurring(dto.getRecurring());
        }
        if (dto.getPaidFlag() != null) {
            existing.setPaidFlag(dto.getPaidFlag());
        }
        if (dto.getIsGroup() != null) {
            existing.setIsGroup(dto.getIsGroup());
        }
        if (dto.getMaxRecurringCycles() != null) {
            existing.setMaxRecurringCycles(dto.getMaxRecurringCycles());
        }
        if (dto.getCycleLengthUnits() != null) {
            existing.setCycleLengthUnits(dto.getCycleLengthUnits());
        }
        if (dto.getCycleLengthType() != null) {
            existing.setCycleLengthType(mapCycleLengthTypeToEntity(dto.getCycleLengthType()));
        }
        if (dto.getExpirationDate() != null) {
            existing.setExpirationDate(toLocalDateTime(dto.getExpirationDate()));
        }
        if (dto.getCustomParameters() != null && !dto.getCustomParameters().isEmpty()) {
            existing.setCustomParameters(serializeCustomParameters(dto.getCustomParameters()));
        }

        return existing;
    }

    // =========================================================================
    // State Mapping Methods
    // =========================================================================

    /**
     * Map entity SubscriptionState to DTO StateEnum
     */
    private com.telecom.ocs.provisioning.api.model.Subscription.StateEnum mapStateToDto(
            Subscription.SubscriptionState entityState) {
        if (entityState == null) {
            return null;
        }

        return switch (entityState) {
            case PENDING -> com.telecom.ocs.provisioning.api.model.Subscription.StateEnum.PENDING;
            case ACTIVE -> com.telecom.ocs.provisioning.api.model.Subscription.StateEnum.ACTIVE;
            case SUSPENDED -> com.telecom.ocs.provisioning.api.model.Subscription.StateEnum.SUSPENDED;
            case CANCELLED -> com.telecom.ocs.provisioning.api.model.Subscription.StateEnum.CANCELLED;
            case EXPIRED -> com.telecom.ocs.provisioning.api.model.Subscription.StateEnum.EXPIRED;
        };
    }

    /**
     * Map DTO StateEnum to entity SubscriptionState
     */
    private Subscription.SubscriptionState mapStateToEntity(
            com.telecom.ocs.provisioning.api.model.Subscription.StateEnum dtoState) {
        if (dtoState == null) {
            return Subscription.SubscriptionState.PENDING; // Default state
        }

        return switch (dtoState) {
            case PENDING -> Subscription.SubscriptionState.PENDING;
            case ACTIVE -> Subscription.SubscriptionState.ACTIVE;
            case SUSPENDED -> Subscription.SubscriptionState.SUSPENDED;
            case CANCELLED -> Subscription.SubscriptionState.CANCELLED;
            case EXPIRED -> Subscription.SubscriptionState.EXPIRED;
        };
    }

    // =========================================================================
    // CycleLengthType Mapping Methods
    // =========================================================================

    /**
     * Map entity CycleLengthType enum to DTO String
     */
    private String mapCycleLengthTypeToDto(Subscription.CycleLengthType cycleLengthType) {
        if (cycleLengthType == null) {
            return null;
        }
        return cycleLengthType.name();
    }

    /**
     * Map DTO cycleLengthType String to entity CycleLengthType enum
     */
    private Subscription.CycleLengthType mapCycleLengthTypeToEntity(String cycleLengthType) {
        if (cycleLengthType == null || cycleLengthType.isBlank()) {
            return null;
        }
        try {
            return Subscription.CycleLengthType.valueOf(cycleLengthType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown cycleLengthType: {}. Returning null.", cycleLengthType);
            return null;
        }
    }

    // =========================================================================
    // Date/Time Conversion Methods
    // =========================================================================

    /**
     * Convert LocalDateTime to OffsetDateTime (Europe/Vienna timezone)
     */
    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZONE_ID).toOffsetDateTime();
    }

    /**
     * Convert OffsetDateTime to LocalDateTime
     */
    private LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(ZONE_ID).toLocalDateTime();
    }

    // =========================================================================
    // Custom Parameters JSON Handling
    // =========================================================================

    /**
     * Parse JSON string to Map for customParameters
     */
    private Map<String, String> parseCustomParameters(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse customParameters JSON: {}. Returning empty map.", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Serialize Map to JSON string for customParameters
     */
    private String serializeCustomParameters(Map<String, String> customParameters) {
        if (customParameters == null || customParameters.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(customParameters);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize customParameters: {}. Returning null.", e.getMessage());
            return null;
        }
    }
}
