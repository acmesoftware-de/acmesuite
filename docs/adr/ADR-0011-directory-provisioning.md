# ADR-0011 — Directory provisioning owned by HRM

- Status: Accepted (2026-08-26)
- Scope: ACMEhr (the `org` module), the directory adapters, deployment configuration
- Supersedes: ADR-0005 (HR to Entra provisioning) — its tenant-specific implementation
- Related: ADR-0007 (federated authentication, local authorization), ADR-0010 (no hard deletes)

## Context

HRM is the system of record for who works here. Joining, moving and leaving each have an
identity side: an account has to exist, follow the person's data, and be blocked when they
leave. Every HR suite does this; it is product functionality, not integration glue.

The first implementation lived in this repository but was written for a single customer's
directory. Its tenant id, application id and that customer's group-naming rules were compiled
in. This repository is a public, generic product, so those specifics had to go, and the whole
implementation went with them — it now runs outside, on the customer side.

What stayed behind is telling. The object id of the person in that directory is still here — at
the time named `Person.entraObjectId`, since renamed to `directoryObjectId` — because it is the
stable SSO anchor that `OrgFeed.subjectRef` resolves against, and so is the narrow write-back
endpoint through which an external run reports the id it obtained. The anchor lives in the product while the thing that writes it lives outside: the seam
is in the wrong place. Any deployment that wants "employee exists, therefore account exists"
currently has to rebuild the mechanism.

Constraints that shape the decision:

- **Public repository.** No tenant, application id, domain or secret may be committed, and
  nothing may be active by default.
- **Directories differ.** Microsoft Graph is the first target; it must not be the only possible
  shape.
- **Authorization stays local** (ADR-0007). Provisioning writes identities outward. It must not
  become a path by which a directory hands out permissions inside the suite.
- **Names are customer vocabulary.** What a group is called, and which local role or power of
  attorney should land in it, differs per customer and must never be a code constant.

## Decision

1. **HRM stays the system of record; the directory is a mirror plus SSO anchor.** The flow is
   one-directional, HRM to directory. The directory is never read back as truth about who
   exists — only the object id returns, as the anchor for sign-in.

2. **A `DirectoryProvisioner` port, expressed in HR terms** rather than directory terms:
   upsert a person, disable a person, set an initial password. It returns a run summary
   (created, updated, disabled, passwords set, per-person errors) so a partially failed run is
   legible instead of being a single boolean.

3. **One adapter per directory, Microsoft Graph first.** The adapter owns the protocol's quirks,
   and the port stays free of them. Two that are already known and must not leak upward:
   - Attributes and the password profile go in **two separate requests**, because they hang on
     two different permissions. Sent together, a directory that has not granted the password
     permission rejects the entire call, and harmless attributes such as the mail address never
     land. Split, a missing password permission costs only the password.
   - On **create** the password travels inline, because the create call requires it.

4. **Idempotent over the login name** (the person's work address), so a run needs no local state
   about what it did last time. The object id the directory returns is written back through the
   existing endpoint, but only when it changed. The anchor is named for the role it plays, not
   for a vendor: `directoryObjectId`.

5. **Group membership comes from a configured mapping, never from code.** A deployment
   configures which local role or power of attorney maps to which directory group name. The
   product ships the mechanism; the deployment owns the vocabulary. The mapping is **empty by
   default**, and an empty mapping means no group is touched at all — a fresh installation
   cannot accidentally rewrite a customer's group memberships.

6. **Only members of the organization are provisioned.** Applicants are candidates in the
   recruiting pipeline, not staff; giving them an account would grant a presence and a
   resolvable sign-in subject to someone who was never hired. They are provisioned when they are
   hired, not before.

7. **Leaving disables, it does not delete** — the identity counterpart of ADR-0010. A departure
   blocks the account and leaves it in place; the account remains as evidence that the person
   existed, and the anchor stays resolvable for historical records.

8. **Inert without configuration.** No bean, no endpoint and no scheduled run exist unless
   provisioning is explicitly switched on, and credentials come only from the environment or a
   secret store. An installation that has not asked for a directory writer must not have one.

## Alternatives

1. **Leave provisioning outside the product (status quo).** Rejected: it makes every deployment
   rebuild the same mechanism, and it leaves the anchor in the product while the writer sits
   outside — the split that motivated this ADR.

2. **Make the directory the system of record and have HRM read it.** Rejected: it hollows out
   the HR domain. `Person` would degrade to a projection of the directory, employment facts
   would have nowhere to live, and history would depend on a system that overwrites in place,
   against ADR-0010.

3. **Implement SCIM instead of per-directory adapters.** Not now, but the port is deliberately
   shaped so a SCIM adapter can sit beside the Graph one. SCIM is the standard for exactly this
   handover, but the common directories consume it rather than serve it — they provision *into*
   applications. Pushing *into* such a directory means its own API, which is what the first
   adapter does.

4. **Reuse the `AuthProvider` SPI from ADR-0007.** Rejected: that SPI is a read path used at
   sign-in, with a different lifecycle and different permissions. A deployment will often point
   both at the same tenant, but they stay separate seams.

## Consequences

- The capability returns to the product, generically. A deployment configures a directory and
  gets joiner/mover/leaver without writing code.
- The customer-side implementation that exists today becomes redundant once the adapter lands.
  Its group rules become configuration; its tenant and application ids become deployment
  settings. Migration is a configuration exercise, not a rewrite.
- HR write paths gain an outward effect. Hiring, changing and deactivating a person now reach an
  external system, which can fail. Failures are reported per person and must not roll back the
  HR change — the local record stays authoritative and the run is repeatable.
- The suite becomes able to set an initial password. That is a sensitive capability and argues
  for keeping the permission that allows it separate and optional, which point 3 already forces.
- A run touches many accounts at once. Group synchronization in particular removes memberships
  the mapping no longer covers, so an incomplete mapping is destructive in an obvious way; the
  empty default and a plan-only mode are what keep that safe.

## Open questions

- Is the initial password shared per deployment or generated per person? A shared value is
  convenient for demonstration systems and poor practice elsewhere.
- Does a run belong behind an operator endpoint, a schedule, or domain events on hire and
  departure? Events are the better fit for a live system, an explicit run for a first cut.
- Which module owns the adapter — the `org` module that holds the persons, or a separate
  integration module beside ACMEbase's providers?
*(Resolved while writing: the anchor was named after one vendor. It turned out the API surface —
endpoint, body field, and the field on `Employee`/`PersonView` — had not shipped: it exists only
in the unmerged change that introduces it, while `main` carried nothing but the internal field
and column. So the rename to `directoryObjectId` costs one commit instead of a deprecation
window, and it happens before the first release rather than as a break after it.)*

*Number provisional: ADR-0008 already exists four times in this log, so confirm the next free
number when this is merged rather than when it is written.*
