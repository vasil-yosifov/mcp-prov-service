# Usage — properties

OpenAPI schema: [`components.schemas.Usage`](../../ocs-provisioing-api.yml#components.schemas.Usage)

## Description

Usage records represent consumption of services (voice calls, data sessions, SMS, MMS) by a subscriber. Each usage record tracks the service consumed, the subscriber being charged, the balance impacted, and the before/after balance values.

## Properties (name : type)

- usageId : string (required) — Unique identifier for the usage record (UUID format)
- usageTimestamp : string (date-time) — Record creation timestamp in ISO 8601 format
- chargedPartyId : string (required) — Subscriber ID being charged
- chargedMsisdn : string — MSISDN being charged in international format, digits only (pattern: ^[0-9]{11,15}$)
- aParty : string — Originating MSISDN in international format (pattern: ^[0-9]{11,15}$)
- bParty : string — Terminating MSISDN or APN (in case of data session)
- usageType : string (required, enum: VOICE, DATA, SMS, MMS) — Type of usage
- recordType : string (required, enum: START, INTERIM, STOP, EVENT) — Type of record
- recordOpeningTime : string (date-time) — Record opening timestamp in ISO 8601 format
- recordClosingTime : string (date-time) — Record closing timestamp in ISO 8601 format
- durationSeconds : integer (minimum: 0) — Record duration in seconds (calculated as closing time minus opening time)
- volumeUsage : number (required, minimum: 0) — Usage volume (bytes for data, count for SMS/MMS, seconds for voice)
- impactedBalanceId : string (required) — ID of the balance impacted by this usage record
- balanceValueBefore : number — Balance available value before usage
- balanceValueAfter : number — Balance available value after usage
- offerId : string — Associated offer ID

## Required Fields

- usageId
- chargedPartyId
- usageType
- recordType
- volumeUsage
- impactedBalanceId

## Enumerations

### usageType
| Value | Description |
|-------|-------------|
| VOICE | Voice call usage |
| DATA | Data/internet session usage |
| SMS | Short Message Service usage |
| MMS | Multimedia Message Service usage |

### recordType
| Value | Description |
|-------|-------------|
| START | Session start record |
| INTERIM | Intermediate record during ongoing session |
| STOP | Session end record |
| EVENT | Single event record (e.g., SMS) |

## Relationships

- **Subscriber** (chargedPartyId → subscriberId): The subscriber being charged for the usage
- **Balance** (impactedBalanceId → balanceId): The balance that is decremented by this usage

## Volume Usage Units

The `volumeUsage` field interpretation depends on the `usageType`:

| usageType | Unit | Example |
|-----------|------|---------|
| VOICE | Seconds | 300 = 5 minute call |
| DATA | Bytes | 104857600 = 100 MB |
| SMS | Count | 1 = one SMS |
| MMS | Count | 1 = one MMS |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /usage | Create a new usage record |

## Example

```json
{
  "usageId": "550e8400-e29b-41d4-a716-446655440000",
  "usageTimestamp": "2024-06-15T10:05:00Z",
  "chargedPartyId": "SUB123456789",
  "chargedMsisdn": "436602238811",
  "aParty": "436602238811",
  "bParty": "436602238822",
  "usageType": "VOICE",
  "recordType": "STOP",
  "recordOpeningTime": "2024-06-15T10:00:00Z",
  "recordClosingTime": "2024-06-15T10:05:00Z",
  "durationSeconds": 300,
  "volumeUsage": 300,
  "impactedBalanceId": "BALANCE123456",
  "balanceValueBefore": 1000,
  "balanceValueAfter": 700,
  "offerId": "OFFER123456"
}
```

## Related Files

- Example instance: [usage.json](../../usage.json)
- OpenAPI specification: [ocs-provisioing-api.yml](../../ocs-provisioing-api.yml)
