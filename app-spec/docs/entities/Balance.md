# Balance — properties

OpenAPI schema: [`components.schemas.Balance`](../ocs-provisioing-api.yml#components.schemas.Balance)

Properties (name : type)

- balanceId : string (required)
- subscriptionId : string
- effectiveDate : string (date-time)
- expirationDate : string (date-time)
- creationDate : string (date-time)
- lastModifiedDate : string (date-time)
- balanceType : string (enum: ALLOWANCE, COUNTER)
- unitType : string (enum: BYTES, SECONDS, EVENTS, MICROCENTS, MICROUNITS)
- balanceAmount : number
- balanceAvailable : number
- isGroupBalance : boolean
- isRecurring : boolean
- cycleLengthType : string (enum: days, months, years)
- cycleLengthUnits : integer
- maxRecurringCycles : integer
- recurringCyclesCompleted : integer
- maxRolloverAmount : number
- rolloverAmount : number
- isRolloverAllowed : boolean
