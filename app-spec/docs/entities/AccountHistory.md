# AccountHistory — properties

OpenAPI schema: [`components.schemas.AccountHistory`](../ocs-provisioing-api.yml#components.schemas.AccountHistory)

Properties (name : type)

- interactionId : string (required)
- entityId : string (required)
- entityType : string (enum: SUBSCRIBER, GROUP, ACCOUNT) (required)
- creationDate : string (date-time) (required)
- description : string
- direction : string
- reason : string
- status : string
- statusChangeDate : string (date-time)
- attachment.id : string
- attachment.url : string
- attachment.type : string
- channel : string
- interactionDate.startDateTime : string (date-time)
- interactionDate.endDateTime : string (date-time)
