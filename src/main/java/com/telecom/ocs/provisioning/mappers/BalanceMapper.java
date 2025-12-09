package com.telecom.ocs.provisioning.mappers;

import com.telecom.ocs.provisioning.models.Balance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between Balance JPA entity and OpenAPI generated DTOs.
 * 
 * Handles:
 * - Enum mapping (balanceType, unitType, cycleLengthType)
 * - LocalDateTime to OffsetDateTime conversion
 * - Entity to DTO and DTO to Entity conversions
 * 
 * Supports task T082 (BalanceMapper for entity ↔ DTO conversion)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceMapper {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Vienna");

    /**
     * Convert Balance entity to OpenAPI DTO
     * 
     * @param entity the JPA entity
     * @return the OpenAPI DTO
     */
    public com.telecom.ocs.provisioning.api.model.Balance toDto(Balance entity) {
        if (entity == null) {
            return null;
        }

        com.telecom.ocs.provisioning.api.model.Balance dto = 
            new com.telecom.ocs.provisioning.api.model.Balance();

        // Basic fields
        dto.setBalanceId(entity.getBalanceId());
        dto.setSubscriptionId(entity.getSubscriptionId());

        // Enum mappings
        dto.setBalanceType(mapBalanceTypeToDto(entity.getBalanceType()));
        dto.setUnitType(mapUnitTypeToDto(entity.getUnitType()));

        // Balance amounts (Long to BigDecimal)
        dto.setBalanceAmount(entity.getBalanceAmount() != null ? BigDecimal.valueOf(entity.getBalanceAmount()) : null);
        dto.setBalanceAvailable(entity.getBalanceAvailable() != null ? BigDecimal.valueOf(entity.getBalanceAvailable()) : null);

        // Date mappings (LocalDateTime to OffsetDateTime)
        dto.setEffectiveDate(toOffsetDateTime(entity.getEffectiveDate()));
        dto.setExpirationDate(toOffsetDateTime(entity.getExpirationDate()));
        dto.setCreationDate(toOffsetDateTime(entity.getCreationDate()));
        dto.setLastModifiedDate(toOffsetDateTime(entity.getLastModifiedDate()));

        // Rollover fields (Long to BigDecimal)
        dto.setIsRolloverAllowed(entity.getIsRolloverAllowed());
        dto.setRolloverAmount(entity.getRolloverAmount() != null ? BigDecimal.valueOf(entity.getRolloverAmount()) : null);
        dto.setMaxRolloverAmount(entity.getMaxRolloverAmount() != null ? BigDecimal.valueOf(entity.getMaxRolloverAmount()) : null);

        // Recurring fields
        dto.setIsRecurring(entity.getIsRecurring());
        dto.setCycleLengthType(mapCycleLengthTypeToDto(entity.getCycleLengthType()));
        dto.setCycleLengthUnits(entity.getCycleLengthUnits());
        dto.setMaxRecurringCycles(entity.getMaxRecurringCycles());
        dto.setRecurringCyclesCompleted(entity.getRecurringCyclesCompleted());

        // Group balance flag
        dto.setIsGroupBalance(entity.getIsGroupBalance());

        return dto;
    }

    /**
     * Convert OpenAPI DTO to Balance entity
     * 
     * @param dto the OpenAPI DTO
     * @return the JPA entity
     */
    public Balance toEntity(com.telecom.ocs.provisioning.api.model.Balance dto) {
        if (dto == null) {
            return null;
        }

        Balance entity = Balance.builder()
                .subscriptionId(dto.getSubscriptionId())
                .balanceAmount(dto.getBalanceAmount() != null ? dto.getBalanceAmount().longValue() : null)
                .balanceAvailable(dto.getBalanceAvailable() != null ? dto.getBalanceAvailable().longValue() : null)
                .isRolloverAllowed(dto.getIsRolloverAllowed() != null ? dto.getIsRolloverAllowed() : false)
                .rolloverAmount(dto.getRolloverAmount() != null ? dto.getRolloverAmount().longValue() : null)
                .maxRolloverAmount(dto.getMaxRolloverAmount() != null ? dto.getMaxRolloverAmount().longValue() : null)
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .cycleLengthUnits(dto.getCycleLengthUnits())
                .maxRecurringCycles(dto.getMaxRecurringCycles())
                .recurringCyclesCompleted(dto.getRecurringCyclesCompleted() != null ? dto.getRecurringCyclesCompleted() : 0)
                .isGroupBalance(dto.getIsGroupBalance() != null ? dto.getIsGroupBalance() : false)
                .build();

        // Enum mappings
        entity.setBalanceType(mapBalanceTypeToEntity(dto.getBalanceType()));
        entity.setUnitType(mapUnitTypeToEntity(dto.getUnitType()));
        entity.setCycleLengthType(mapCycleLengthTypeToEntity(dto.getCycleLengthType()));

        // Date mappings (OffsetDateTime to LocalDateTime)
        entity.setEffectiveDate(toLocalDateTime(dto.getEffectiveDate()));
        entity.setExpirationDate(toLocalDateTime(dto.getExpirationDate()));

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
    public Balance updateEntityFromDto(Balance existing, 
            com.telecom.ocs.provisioning.api.model.Balance dto) {
        if (dto == null) {
            return existing;
        }

        // Only update non-null fields from DTO
        if (dto.getBalanceAmount() != null) {
            existing.setBalanceAmount(dto.getBalanceAmount().longValue());
        }
        if (dto.getBalanceAvailable() != null) {
            existing.setBalanceAvailable(dto.getBalanceAvailable().longValue());
        }
        if (dto.getEffectiveDate() != null) {
            existing.setEffectiveDate(toLocalDateTime(dto.getEffectiveDate()));
        }
        if (dto.getExpirationDate() != null) {
            existing.setExpirationDate(toLocalDateTime(dto.getExpirationDate()));
        }
        if (dto.getIsRolloverAllowed() != null) {
            existing.setIsRolloverAllowed(dto.getIsRolloverAllowed());
        }
        if (dto.getRolloverAmount() != null) {
            existing.setRolloverAmount(dto.getRolloverAmount().longValue());
        }
        if (dto.getMaxRolloverAmount() != null) {
            existing.setMaxRolloverAmount(dto.getMaxRolloverAmount().longValue());
        }
        if (dto.getIsRecurring() != null) {
            existing.setIsRecurring(dto.getIsRecurring());
        }
        if (dto.getCycleLengthType() != null) {
            existing.setCycleLengthType(mapCycleLengthTypeToEntity(dto.getCycleLengthType()));
        }
        if (dto.getCycleLengthUnits() != null) {
            existing.setCycleLengthUnits(dto.getCycleLengthUnits());
        }
        if (dto.getMaxRecurringCycles() != null) {
            existing.setMaxRecurringCycles(dto.getMaxRecurringCycles());
        }
        if (dto.getIsGroupBalance() != null) {
            existing.setIsGroupBalance(dto.getIsGroupBalance());
        }

        return existing;
    }

    // =========================================================================
    // Balance Type String Mapping Methods
    // =========================================================================

    /**
     * Map entity BalanceType to DTO String
     */
    private String mapBalanceTypeToDto(Balance.BalanceType entityType) {
        if (entityType == null) {
            return null;
        }
        return entityType.name();
    }

    /**
     * Map DTO String to entity BalanceType
     */
    private Balance.BalanceType mapBalanceTypeToEntity(String dtoType) {
        if (dtoType == null || dtoType.isBlank()) {
            return Balance.BalanceType.ALLOWANCE; // Default type
        }
        try {
            return Balance.BalanceType.valueOf(dtoType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown balanceType: {}. Returning default ALLOWANCE.", dtoType);
            return Balance.BalanceType.ALLOWANCE;
        }
    }

    // =========================================================================
    // Unit Type Enum Mapping Methods
    // =========================================================================

    /**
     * Map entity UnitType to DTO UnitTypeEnum
     */
    private com.telecom.ocs.provisioning.api.model.Balance.UnitTypeEnum mapUnitTypeToDto(
            Balance.UnitType entityType) {
        if (entityType == null) {
            return null;
        }

        return switch (entityType) {
            case BYTES -> com.telecom.ocs.provisioning.api.model.Balance.UnitTypeEnum.BYTES;
            case SECONDS -> com.telecom.ocs.provisioning.api.model.Balance.UnitTypeEnum.SECONDS;
            case EVENTS -> com.telecom.ocs.provisioning.api.model.Balance.UnitTypeEnum.EVENTS;
            case MICROCENTS -> com.telecom.ocs.provisioning.api.model.Balance.UnitTypeEnum.MICROCENTS;
            case MICROUNITS -> com.telecom.ocs.provisioning.api.model.Balance.UnitTypeEnum.MICROUNITS;
        };
    }

    /**
     * Map DTO UnitTypeEnum to entity UnitType
     */
    private Balance.UnitType mapUnitTypeToEntity(
            com.telecom.ocs.provisioning.api.model.Balance.UnitTypeEnum dtoType) {
        if (dtoType == null) {
            return Balance.UnitType.BYTES; // Default unit type
        }

        return switch (dtoType) {
            case BYTES -> Balance.UnitType.BYTES;
            case SECONDS -> Balance.UnitType.SECONDS;
            case EVENTS -> Balance.UnitType.EVENTS;
            case MICROCENTS -> Balance.UnitType.MICROCENTS;
            case MICROUNITS -> Balance.UnitType.MICROUNITS;
        };
    }

    // =========================================================================
    // CycleLengthType Mapping Methods (from Subscription entity)
    // =========================================================================

    /**
     * Map entity CycleLengthType enum to DTO String
     */
    private String mapCycleLengthTypeToDto(com.telecom.ocs.provisioning.models.Subscription.CycleLengthType cycleLengthType) {
        if (cycleLengthType == null) {
            return null;
        }
        return cycleLengthType.name();
    }

    /**
     * Map DTO cycleLengthType String to entity CycleLengthType enum
     */
    private com.telecom.ocs.provisioning.models.Subscription.CycleLengthType mapCycleLengthTypeToEntity(String cycleLengthType) {
        if (cycleLengthType == null || cycleLengthType.isBlank()) {
            return null;
        }
        try {
            return com.telecom.ocs.provisioning.models.Subscription.CycleLengthType.valueOf(cycleLengthType.toUpperCase());
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
}
