# Group — properties

OpenAPI schema: [`components.schemas.Group`](../ocs-provisioing-api.yml#components.schemas.Group)

Properties (name : type)

- groupId : string (required)
- groupName : string
- groupType : string
- state : string (enum: active, suspended, terminated)
- createdDate : string (date-time)
- modifiedDate : string (date-time)
- expirationDate : string (date-time)

groupOwner
- groupOwner.subscriberId : string
- groupOwner.role : string (enum: ADMIN, MEMBER)
- groupOwner.memberSince : string (date-time)

- members[] : string
- subscriptions[] : string
- notificationAddresses[] : string

settings
- settings.maxMembers : integer
- settings.autoRenew : boolean
- settings.quotaDistribution : string

- customFields.* : string
- lastModifiedDate : string (date-time)
- timers[] : string
