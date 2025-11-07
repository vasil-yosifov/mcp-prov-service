# Subscription — properties

OpenAPI schema: [`components.schemas.Subscription`](../ocs-provisioing-api.yml#components.schemas.Subscription)

Properties (name : type)

- subscriptionId : string (required)
- subscriberId : string
- subscriptionType : string
- offerId : string
- offerName : string
- state : string (enum: pending, active, suspended, cancelled, expired)
- creationDate : string (date-time)
- activationDate : string (date-time)
- expirationDate : string (date-time)
- renewalDate : string (date-time)
- recurring : boolean
- paidFlag : boolean
- isGroup : boolean
- maxRecurringCycles : integer
- recurringCyclesCompleted : integer
- cycleLengthUnits : integer
- cycleLengthType : string
- customParameters.* : string
- balances[] : string
- lastModifiedDate : string (date-time)
- timers[] : string
