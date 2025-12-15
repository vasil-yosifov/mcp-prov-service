package com.telecom.ocs.provisioning.mappers;

import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.models.Subscription;
import com.telecom.ocs.provisioning.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Subscriber JPA entity and OpenAPI generated DTOs.
 * 
 * Simplified version that handles basic mapping.
 * For production use, consider using MapStruct or ModelMapper.
 */
@Component
@RequiredArgsConstructor
public class SubscriberMapper {

    private final SubscriptionRepository subscriptionRepository;

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Vienna");

    /**
     * Convert Subscriber entity to OpenAPI DTO (simplified version)
     * 
     * @param entity the JPA entity
     * @return the OpenAPI DTO
     */
    public com.telecom.ocs.provisioning.api.model.Subscriber toDto(Subscriber entity) {
        if (entity == null) {
            return null;
        }

        com.telecom.ocs.provisioning.api.model.Subscriber dto = 
            new com.telecom.ocs.provisioning.api.model.Subscriber();

        dto.setSubscriberId(entity.getSubscriberId());
        dto.setMsisdn(entity.getMsisdn());
        dto.setImsi(entity.getImsi());
        dto.setLanguageId(entity.getLanguageId());
        dto.setCarrierId(entity.getCarrierId());
        dto.setSubscriberType(entity.getSubscriberType());
        
        // Personal info
        if (entity.getFirstName() != null || entity.getLastName() != null || 
            entity.getDateOfBirth() != null || entity.getEmail() != null) {
            com.telecom.ocs.provisioning.api.model.SubscriberPersonalInfo personalInfo = 
                new com.telecom.ocs.provisioning.api.model.SubscriberPersonalInfo();
            personalInfo.setFirstName(entity.getFirstName());
            personalInfo.setLastName(entity.getLastName());
            personalInfo.setDateOfBirth(entity.getDateOfBirth());
            personalInfo.setEmail(entity.getEmail());
            personalInfo.setContactNumber(entity.getContactNumber());
            dto.setPersonalInfo(personalInfo);
        }

        // Billing info
        if (entity.getBillingCycle() != null || entity.getBillingStreet() != null || 
            entity.getBillingCity() != null || entity.getBillingCountry() != null) {
            com.telecom.ocs.provisioning.api.model.SubscriberBilling billing = 
                new com.telecom.ocs.provisioning.api.model.SubscriberBilling();
            billing.setBillingCycle(entity.getBillingCycle());
            billing.setBillcycleDay(entity.getBillcycleDay());
            
            if (entity.getBillingStreet() != null || entity.getBillingCity() != null ||
                entity.getBillingState() != null || entity.getBillingZipCode() != null ||
                entity.getBillingCountry() != null) {
                com.telecom.ocs.provisioning.api.model.SubscriberBillingBillingAddress address = 
                    new com.telecom.ocs.provisioning.api.model.SubscriberBillingBillingAddress();
                address.setStreet(entity.getBillingStreet());
                address.setCity(entity.getBillingCity());
                address.setState(entity.getBillingState());
                address.setZipCode(entity.getBillingZipCode());
                address.setCountry(entity.getBillingCountry());
                billing.setBillingAddress(address);
            }
            
            dto.setBilling(billing);
        }

        // State mappings
        dto.setCurrentState(mapStateToCurrentDto(entity.getState()));
        dto.setPreviousState(mapStateToPreviousDto(entity.getPreviousState()));
        
        if (entity.getLastTransitionDate() != null) {
            dto.setLastTransitionDate(toOffsetDateTime(entity.getLastTransitionDate()));
        }
        
        if (entity.getActivationDate() != null) {
            dto.setActivationDate(toOffsetDateTime(entity.getActivationDate()));
        }
        
        if (entity.getExpirationDate() != null) {
            dto.setExpirationDate(toOffsetDateTime(entity.getExpirationDate()));
        }
        
        if (entity.getCreationDate() != null) {
            dto.setCreationDate(toOffsetDateTime(entity.getCreationDate()));
        }
        
        if (entity.getLastModifiedDate() != null) {
            dto.setLastModifiedDate(toOffsetDateTime(entity.getLastModifiedDate()));
        }

        // Fetch and set subscription IDs for this subscriber
        List<Subscription> subscriptions = subscriptionRepository.findBySubscriberId(entity.getSubscriberId());
        if (subscriptions != null && !subscriptions.isEmpty()) {
            List<String> subscriptionIds = subscriptions.stream()
                    .map(Subscription::getSubscriptionId)
                    .collect(Collectors.toList());
            dto.setSubscriptions(subscriptionIds);
        }

        return dto;
    }

    /**
     * Convert OpenAPI DTO to Subscriber entity (simplified version)
     * 
     * @param dto the OpenAPI DTO
     * @return the JPA entity
     */
    public Subscriber toEntity(com.telecom.ocs.provisioning.api.model.Subscriber dto) {
        if (dto == null) {
            return null;
        }

        // Don't set subscriberId from DTO - it's generated by the service
        Subscriber entity = Subscriber.builder()
                .msisdn(dto.getMsisdn())
                .imsi(dto.getImsi())
                .languageId(dto.getLanguageId())
                .carrierId(dto.getCarrierId())
                .subscriberType(dto.getSubscriberType())
                .build();

        // Personal info
        if (dto.getPersonalInfo() != null) {
            entity.setFirstName(dto.getPersonalInfo().getFirstName());
            entity.setLastName(dto.getPersonalInfo().getLastName());
            entity.setDateOfBirth(dto.getPersonalInfo().getDateOfBirth());
            entity.setEmail(dto.getPersonalInfo().getEmail());
            entity.setContactNumber(dto.getPersonalInfo().getContactNumber());
        }

        // Billing info
        if (dto.getBilling() != null) {
            entity.setBillingCycle(dto.getBilling().getBillingCycle());
            entity.setBillcycleDay(dto.getBilling().getBillcycleDay());
            
            if (dto.getBilling().getBillingAddress() != null) {
                entity.setBillingStreet(dto.getBilling().getBillingAddress().getStreet());
                entity.setBillingCity(dto.getBilling().getBillingAddress().getCity());
                entity.setBillingState(dto.getBilling().getBillingAddress().getState());
                entity.setBillingZipCode(dto.getBilling().getBillingAddress().getZipCode());
                entity.setBillingCountry(dto.getBilling().getBillingAddress().getCountry());
            }
        }

        // State mapping - set default if not provided
        if (dto.getCurrentState() != null) {
            entity.setState(mapCurrentStateToEntity(dto.getCurrentState()));
        }

        // Date mappings (from DTO to entity)
        if (dto.getActivationDate() != null) {
            entity.setActivationDate(toLocalDateTime(dto.getActivationDate()));
        }
        
        if (dto.getExpirationDate() != null) {
            entity.setExpirationDate(toLocalDateTime(dto.getExpirationDate()));
        }

        return entity;
    }

    /**
     * Map entity state to DTO CurrentStateEnum
     */
    private com.telecom.ocs.provisioning.api.model.Subscriber.CurrentStateEnum mapStateToCurrentDto(
            Subscriber.SubscriberState entityState) {
        if (entityState == null) {
            return null;
        }

        switch (entityState) {
            case PRE_PROVISIONED:
                return com.telecom.ocs.provisioning.api.model.Subscriber.CurrentStateEnum.PRE_PROVISIONED;
            case ACTIVE:
                return com.telecom.ocs.provisioning.api.model.Subscriber.CurrentStateEnum.ACTIVE;
            case SUSPENDED:
                return com.telecom.ocs.provisioning.api.model.Subscriber.CurrentStateEnum.SUSPENDED;
            case DEACTIVATED:
                return com.telecom.ocs.provisioning.api.model.Subscriber.CurrentStateEnum.DEACTIVATED;
            case TERMINATED:
                return com.telecom.ocs.provisioning.api.model.Subscriber.CurrentStateEnum.TERMINATED;
            default:
                return null;
        }
    }

    /**
     * Map entity state to DTO PreviousStateEnum
     */
    private com.telecom.ocs.provisioning.api.model.Subscriber.PreviousStateEnum mapStateToPreviousDto(
            Subscriber.SubscriberState entityState) {
        if (entityState == null) {
            return null;
        }

        switch (entityState) {
            case PRE_PROVISIONED:
                return com.telecom.ocs.provisioning.api.model.Subscriber.PreviousStateEnum.PRE_PROVISIONED;
            case ACTIVE:
                return com.telecom.ocs.provisioning.api.model.Subscriber.PreviousStateEnum.ACTIVE;
            case SUSPENDED:
                return com.telecom.ocs.provisioning.api.model.Subscriber.PreviousStateEnum.SUSPENDED;
            case DEACTIVATED:
                return com.telecom.ocs.provisioning.api.model.Subscriber.PreviousStateEnum.DEACTIVATED;
            case TERMINATED:
                return com.telecom.ocs.provisioning.api.model.Subscriber.PreviousStateEnum.TERMINATED;
            default:
                return null;
        }
    }

    /**
     * Map DTO CurrentStateEnum to entity state
     */
    private Subscriber.SubscriberState mapCurrentStateToEntity(
            com.telecom.ocs.provisioning.api.model.Subscriber.CurrentStateEnum dtoState) {
        if (dtoState == null) {
            return null;
        }

        switch (dtoState) {
            case PRE_PROVISIONED:
                return Subscriber.SubscriberState.PRE_PROVISIONED;
            case ACTIVE:
                return Subscriber.SubscriberState.ACTIVE;
            case SUSPENDED:
                return Subscriber.SubscriberState.SUSPENDED;
            case DEACTIVATED:
                return Subscriber.SubscriberState.DEACTIVATED;
            case TERMINATED:
                return Subscriber.SubscriberState.TERMINATED;
            default:
                return null;
        }
    }

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

