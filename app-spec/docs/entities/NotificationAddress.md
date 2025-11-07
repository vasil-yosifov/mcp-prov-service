# NotificationAddress — properties

OpenAPI schema: [`components.schemas.NotificationAddress`](../ocs-provisioing-api.yml#components.schemas.NotificationAddress)

Properties (name : type)

- notificationAddressId : string (required)
- notificationType : string (enum: SMS, EMAIL)
- notificationAddress : string (msisdn or email)
- lastNotificationDate : string (date-time)
- createdDate : string (date-time)
- modifiedDate : string (date-time)
- expirationDate : string (date-time)
