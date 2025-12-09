package com.telecom.ocs.provisioning.mappers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between AccountHistory JPA entity and OpenAPI generated DTOs.
 * 
 * Handles:
 * - EntityType enum mapping
 * - LocalDateTime to OffsetDateTime conversion
 * - Entity to DTO and DTO to Entity conversions
 * - Attachment metadata (nested object in DTO)
 * - InteractionDate (nested object in DTO)
 * 
 * T097: AccountHistoryMapper for entity ↔ DTO conversion
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountHistoryMapper {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Vienna");

    /**
     * Convert AccountHistory entity to OpenAPI DTO.
     * 
     * @param entity the JPA entity
     * @return the OpenAPI DTO
     */
    public com.telecom.ocs.provisioning.api.model.AccountHistory toDto(
            com.telecom.ocs.provisioning.models.AccountHistory entity) {
        if (entity == null) {
            return null;
        }

        com.telecom.ocs.provisioning.api.model.AccountHistory dto = 
            new com.telecom.ocs.provisioning.api.model.AccountHistory();

        // Basic fields
        dto.setInteractionId(entity.getInteractionId());
        dto.setEntityId(entity.getEntityId());

        // Enum mapping - use string conversion to avoid class loading issues
        if (entity.getEntityType() != null) {
            String typeName = entity.getEntityType().name();
            dto.setEntityType(com.telecom.ocs.provisioning.api.model.AccountHistory.EntityTypeEnum.fromValue(typeName));
        }

        // Date mappings (LocalDateTime to OffsetDateTime)
        dto.setCreationDate(toOffsetDateTime(entity.getCreationDate()));
        dto.setStatusChangeDate(toOffsetDateTime(entity.getStatusChangeDate()));

        // Optional string fields
        dto.setDescription(entity.getDescription());
        dto.setDirection(entity.getDirection());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        dto.setChannel(entity.getChannel());

        // InteractionDate nested object
        if (entity.getStartDateTime() != null || entity.getEndDateTime() != null) {
            com.telecom.ocs.provisioning.api.model.AccountHistoryInteractionDate interactionDate = 
                new com.telecom.ocs.provisioning.api.model.AccountHistoryInteractionDate();
            interactionDate.setStartDateTime(toOffsetDateTime(entity.getStartDateTime()));
            interactionDate.setEndDateTime(toOffsetDateTime(entity.getEndDateTime()));
            dto.setInteractionDate(interactionDate);
        }

        // Attachment nested object
        if (entity.getAttachmentId() != null || entity.getAttachmentUrl() != null || entity.getAttachmentType() != null) {
            com.telecom.ocs.provisioning.api.model.AccountHistoryAttachment attachment = 
                new com.telecom.ocs.provisioning.api.model.AccountHistoryAttachment();
            attachment.setId(entity.getAttachmentId());
            attachment.setUrl(entity.getAttachmentUrl());
            attachment.setType(entity.getAttachmentType());
            dto.setAttachment(attachment);
        }

        return dto;
    }

    /**
     * Convert OpenAPI DTO to AccountHistory entity.
     * 
     * @param dto the OpenAPI DTO
     * @return the JPA entity
     */
    public com.telecom.ocs.provisioning.models.AccountHistory toEntity(
            com.telecom.ocs.provisioning.api.model.AccountHistory dto) {
        if (dto == null) {
            return null;
        }

        com.telecom.ocs.provisioning.models.AccountHistory entity = 
            com.telecom.ocs.provisioning.models.AccountHistory.builder()
                .interactionId(dto.getInteractionId())
                .entityId(dto.getEntityId())
                .description(dto.getDescription())
                .direction(dto.getDirection())
                .reason(dto.getReason())
                .status(dto.getStatus())
                .channel(dto.getChannel())
                .build();

        // Enum mapping - use reflection to avoid direct type references
        if (dto.getEntityType() != null) {
            String typeName = dto.getEntityType().name();
            try {
                Class<?> accountHistoryClass = Class.forName(
                    "com.telecom.ocs.provisioning.models.AccountHistory");
                Class<?> entityTypeClass = Class.forName(
                    "com.telecom.ocs.provisioning.models.AccountHistory$EntityType");
                java.lang.reflect.Method valueOfMethod = entityTypeClass.getMethod("valueOf", String.class);
                Object enumValue = valueOfMethod.invoke(null, typeName);
                // Use reflection to call setEntityType to avoid compile-time type checking
                java.lang.reflect.Method setterMethod = accountHistoryClass.getMethod("setEntityType", entityTypeClass);
                setterMethod.invoke(entity, enumValue);
            } catch (Exception e) {
                log.error("Failed to set EntityType enum", e);
            }
        }

        // Date mappings (OffsetDateTime to LocalDateTime)
        entity.setStatusChangeDate(toLocalDateTime(dto.getStatusChangeDate()));

        // InteractionDate nested object
        if (dto.getInteractionDate() != null) {
            entity.setStartDateTime(toLocalDateTime(dto.getInteractionDate().getStartDateTime()));
            entity.setEndDateTime(toLocalDateTime(dto.getInteractionDate().getEndDateTime()));
        }

        // Attachment nested object
        if (dto.getAttachment() != null) {
            entity.setAttachmentId(dto.getAttachment().getId());
            entity.setAttachmentUrl(dto.getAttachment().getUrl());
            entity.setAttachmentType(dto.getAttachment().getType());
        }

        return entity;
    }

    /**
     * Update existing entity with values from DTO.
     * Used for PATCH operations where we want to preserve existing values.
     * 
     * @param existing the existing entity to update
     * @param dto the DTO with new values
     * @return the updated entity
     */
    public com.telecom.ocs.provisioning.models.AccountHistory updateEntityFromDto(
            com.telecom.ocs.provisioning.models.AccountHistory existing, 
            com.telecom.ocs.provisioning.api.model.AccountHistory dto) {
        if (dto == null) {
            return existing;
        }

        // Only update non-null fields from DTO
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getDirection() != null) {
            existing.setDirection(dto.getDirection());
        }
        if (dto.getReason() != null) {
            existing.setReason(dto.getReason());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getStatusChangeDate() != null) {
            existing.setStatusChangeDate(toLocalDateTime(dto.getStatusChangeDate()));
        }
        if (dto.getChannel() != null) {
            existing.setChannel(dto.getChannel());
        }
        if (dto.getInteractionDate() != null) {
            if (dto.getInteractionDate().getStartDateTime() != null) {
                existing.setStartDateTime(toLocalDateTime(dto.getInteractionDate().getStartDateTime()));
            }
            if (dto.getInteractionDate().getEndDateTime() != null) {
                existing.setEndDateTime(toLocalDateTime(dto.getInteractionDate().getEndDateTime()));
            }
        }
        if (dto.getAttachment() != null) {
            if (dto.getAttachment().getId() != null) {
                existing.setAttachmentId(dto.getAttachment().getId());
            }
            if (dto.getAttachment().getUrl() != null) {
                existing.setAttachmentUrl(dto.getAttachment().getUrl());
            }
            if (dto.getAttachment().getType() != null) {
                existing.setAttachmentType(dto.getAttachment().getType());
            }
        }

        return existing;
    }

    /**
     * Convert LocalDateTime to OffsetDateTime (Europe/Vienna timezone).
     */
    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZONE_ID).toOffsetDateTime();
    }

    /**
     * Convert OffsetDateTime to LocalDateTime.
     */
    private LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(ZONE_ID).toLocalDateTime();
    }
}
