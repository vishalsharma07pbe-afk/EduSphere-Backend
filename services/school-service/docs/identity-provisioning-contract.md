# Identity-Service Internal Provisioning Contract

`school-service` calls this endpoint after the school and provisioning record commit.

## Request

`POST /internal/v1/school-provisioning/initial-authority`

Headers:

`Idempotency-Key: school-provisioning:{schoolId}:{provisioningId}`

Body:

```json
{
  "organizationId": 123,
  "name": "Authority Name",
  "username": "authority.admin",
  "email": "authority@example.com",
  "phone": "+911234567890",
  "role": "GOVERNING_AUTHORITY"
}
```

`organizationId` is the saved school ID. `role` is always server-derived by `school-service`.
The request never includes a password, token, or caller-supplied organization ID.

## Response

Return any 2xx status when the authority exists or was created for the idempotency key.
Return non-2xx for retryable or permanent failures. Error bodies must not require
`school-service` to persist secrets; `school-service` stores only a 500-character safe summary.

Identity-service must make repeated requests with the same `Idempotency-Key` safe and must
not create duplicate authorities.
