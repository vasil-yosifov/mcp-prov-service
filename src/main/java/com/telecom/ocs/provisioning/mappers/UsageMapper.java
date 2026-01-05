package com.telecom.ocs.provisioning.mappers;

import com.telecom.ocs.provisioning.models.Usage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Usage JPA entity and OpenAPI generated DTOs.
 * 
 * Handles:
 * - Entity to DTO conversion for API responses
 * - DTO to Entity conversion for API requests
 * - LocalDateTime to OffsetDateTime conversion
 * - BigDecimal to Long conversion for volumes
 * - Enum mappings
 * 
 * T181: UsageMapper for entity ↔ DTO conversion
 */
@Component
public class UsageMapper {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Vienna");

    /**
     * Convert Usage entity to OpenAPI DTO
     * 
     * @param entity the JPA entity
     * @return the OpenAPI DTO
     */
    public com.telecom.ocs.provisioning.api.model.Usage toDto(Usage entity) {
        if (entity == null) {
            return null;
        }

        com.telecom.ocs.provisioning.api.model.Usage dto = 
            new com.telecom.ocs.provisioning.api.model.Usage();

        // Required fields
        dto.setUsageId(entity.getUsageId());
        dto.setChargedPartyId(entity.getChargedPartyId());
        dto.setUsageType(mapUsageTypeToDto(entity.getUsageType()));
        dto.setRecordType(mapRecordTypeToDto(entity.getRecordType()));
        dto.setVolumeUsage(BigDecimal.valueOf(entity.getVolumeUsage()));
        dto.setImpactedBalanceId(entity.getImpactedBalanceId());

        // Optional fields
        if (entity.getUsageTimestamp() != null) {
            dto.setUsageTimestamp(localDateTimeToOffsetDateTime(entity.getUsageTimestamp()));
        }
        dto.setChargedMsisdn(entity.getChargedMsisdn());
        dto.setaParty(entity.getAParty());
        dto.setbParty(entity.getBParty());
        
        if (entity.getRecordOpeningTime() != null) {
            dto.setRecordOpeningTime(localDateTimeToOffsetDateTime(entity.getRecordOpeningTime()));
        }
        if (entity.getRecordClosingTime() != null) {
            dto.setRecordClosingTime(localDateTimeToOffsetDateTime(entity.getRecordClosingTime()));
        }
        dto.setDurationSeconds(entity.getDurationSeconds());
        
        if (entity.getBalanceValueBefore() != null) {
            dto.setBalanceValueBefore(BigDecimal.valueOf(entity.getBalanceValueBefore()));
        }
        if (entity.getBalanceValueAfter() != null) {
            dto.setBalanceValueAfter(BigDecimal.valueOf(entity.getBalanceValueAfter()));
        }
        dto.setOfferId(entity.getOfferId());

        return dto;
    }

    /**
     * Convert OpenAPI DTO to Usage entity
     * 
     * @param dto the OpenAPI DTO
     * @return the JPA entity
     */
    public Usage toEntity(com.telecom.ocs.provisioning.api.model.Usage dto) {
        if (dto == null) {
            return null;
        }

        Usage entity = new Usage();

        // Set or generate usageId
        if (dto.getUsageId() != null && !dto.getUsageId().isEmpty()) {
            entity.setUsageId(dto.getUsageId());
        } else {
            entity.setUsageId(UUID.randomUUID().toString());
        }

        // Required fields
        entity.setChargedPartyId(dto.getChargedPartyId());
        entity.setUsageType(mapUsageTypeToEntity(dto.getUsageType()));
        entity.setRecordType(mapRecordTypeToEntity(dto.getRecordType()));
        entity.setVolumeUsage(dto.getVolumeUsage().longValue());
        entity.setImpactedBalanceId(dto.getImpactedBalanceId());

        // Optional fields
        if (dto.getUsageTimestamp() != null) {
            entity.setUsageTimestamp(offsetDateTimeToLocalDateTime(dto.getUsageTimestamp()));
        }
        entity.setChargedMsisdn(dto.getChargedMsisdn());
        entity.setAParty(dto.getaParty());
        entity.setBParty(dto.getbParty());
        
        if (dto.getRecordOpeningTime() != null) {
            entity.setRecordOpeningTime(offsetDateTimeToLocalDateTime(dto.getRecordOpeningTime()));
        }
        if (dto.getRecordClosingTime() != null) {
            entity.setRecordClosingTime(offsetDateTimeToLocalDateTime(dto.getRecordClosingTime()));
        }
        entity.setDurationSeconds(dto.getDurationSeconds());
        
        if (dto.getBalanceValueBefore() != null) {
            entity.setBalanceValueBefore(dto.getBalanceValueBefore().longValue());
        }
        if (dto.getBalanceValueAfter() != null) {
            entity.setBalanceValueAfter(dto.getBalanceValueAfter().longValue());
        }
        entity.setOfferId(dto.getOfferId());

        return entity;
    }

    /**
     * Convert list of entities to DTOs
     */
    public List<com.telecom.ocs.provisioning.api.model.Usage> toDtoList(List<Usage> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Map entity UsageType to DTO UsageTypeEnum
     */
    private com.telecom.ocs.provisioning.api.model.Usage.UsageTypeEnum mapUsageTypeToDto(Usage.UsageType usageType) {
        if (usageType == null) {
            return null;
        }
        switch (usageType) {
            case VOICE:
                return com.telecom.ocs.provisioning.api.model.Usage.UsageTypeEnum.VOICE;
            case DATA:
                return com.telecom.ocs.provisioning.api.model.Usage.UsageTypeEnum.DATA;
            case SMS:
                return com.telecom.ocs.provisioning.api.model.Usage.UsageTypeEnum.SMS;
            case MMS:
                return com.telecom.ocs.provisioning.api.model.Usage.UsageTypeEnum.MMS;
            default:
                throw new IllegalArgumentException("Unknown UsageType: " + usageType);
        }
    }

    /**
     * Map DTO UsageTypeEnum to entity UsageType
     */
    private Usage.UsageType mapUsageTypeToEntity(com.telecom.ocs.provisioning.api.model.Usage.UsageTypeEnum usageType) {
        if (usageType == null) {
            return null;
        }
        switch (usageType) {
            case VOICE:
                return Usage.UsageType.VOICE;
            case DATA:
                return Usage.UsageType.DATA;
            case SMS:
                return Usage.UsageType.SMS;
            case MMS:
                return Usage.UsageType.MMS;
            default:
                throw new IllegalArgumentException("Unknown UsageTypeEnum: " + usageType);
        }
    }

    /**
     * Map entity RecordType to DTO RecordTypeEnum
     */
    private com.telecom.ocs.provisioning.api.model.Usage.RecordTypeEnum mapRecordTypeToDto(Usage.RecordType recordType) {
        if (recordType == null) {
            return null;
        }
        switch (recordType) {
            case START:
                return com.telecom.ocs.provisioning.api.model.Usage.RecordTypeEnum.START;
            case INTERIM:
                return com.telecom.ocs.provisioning.api.model.Usage.RecordTypeEnum.INTERIM;
            case STOP:
                return com.telecom.ocs.provisioning.api.model.Usage.RecordTypeEnum.STOP;
            case EVENT:
                return com.telecom.ocs.provisioning.api.model.Usage.RecordTypeEnum.EVENT;
            default:
                throw new IllegalArgumentException("Unknown RecordType: " + recordType);
        }
    }

    /**
     * Map DTO RecordTypeEnum to entity RecordType
     */
    private Usage.RecordType mapRecordTypeToEntity(com.telecom.ocs.provisioning.api.model.Usage.RecordTypeEnum recordType) {
        if (recordType == null) {
            return null;
        }
        switch (recordType) {
            case START:
                return Usage.RecordType.START;
            case INTERIM:
                return Usage.RecordType.INTERIM;
            case STOP:
                return Usage.RecordType.STOP;
            case EVENT:
                return Usage.RecordType.EVENT;
            default:
                throw new IllegalArgumentException("Unknown RecordTypeEnum: " + recordType);
        }
    }

    /**
     * Convert LocalDateTime to OffsetDateTime
     */
    private OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZONE_ID).toOffsetDateTime();
    }

    /**
     * Convert OffsetDateTime to LocalDateTime
     */
    private LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(ZONE_ID).toLocalDateTime();
    }
}
