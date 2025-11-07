# Subscriber — properties

OpenAPI schema: [`components.schemas.Subscriber`](../ocs-provisioing-api.yml#components.schemas.Subscriber)

Properties (name : type)

- subscriberId : string (required)
- businessAccountId : string
- msisdn : string
- imsi : string
- iccId : string
- currentState : string (enum: pre-provisioned, active, suspended, deactivated, terminated)
- previousState : string
- creationDate : string (date-time)
- lastTransitionDate : string (date-time)
- activationDate : string (date-time)
- expirationDate : string (date-time)
- languageId : string
- carrierId : string
- subscriberType : string

personalInfo
- personalInfo.firstName : string
- personalInfo.lastName : string
- personalInfo.dateOfBirth : string (date)
- personalInfo.email : string
- personalInfo.contactNumber : string

billing
- billing.billingCycle : string
- billing.billcycleDay : integer
- billing.billingAddress.street : string
- billing.billingAddress.city : string
- billing.billingAddress.state : string
- billing.billingAddress.zipCode : string
- billing.billingAddress.country : string

- groups[] : string
- subscriptions[] : string
- notificationAddresses[] : string

services
- services.voice : boolean
- services.sms : boolean
- services.mms : boolean
- services.data : boolean
- services.roaming : boolean
- services.valueAddedServices[] : string

- customFields.* : string
- lastModifiedDate : string (date-time)
- timers[] : string
