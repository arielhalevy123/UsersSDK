# UsersSDK — Improvement Plan

Ordered by **grade impact per hour**. Each item is independent; stop whenever you run out of time.

Current state: 20 source files, Spring Boot + JWT + PostgreSQL, Android SDK module, admin portal,
good architecture docs. **One test file containing only `contextLoads()`.**

---

## Tier 1 — do these three, they carry the most weight

### 1.1 Reproduce the lost-update bug with a test  ⏱ ~1h  🎯 highest impact
The README already admits appointments are a semicolon-separated string in `UserCustomField.fieldValue`.
There is **no `@Version`, no optimistic locking, no `@Transactional` on the write path.**

Two concurrent bookings therefore both read the string, each appends, and the second write erases the
first booking — silently. For a barbershop queue that is the core use case, not an edge case.

A test that *reproduces* this is the single most valuable artefact you can add. Examiners reward a
student who can demonstrate where their own system breaks.

### 1.2 Fix it with optimistic locking  ⏱ ~15min
Add to `UserCustomField`:
```java
@Version
private Long version;
```
The same test then throws `ObjectOptimisticLockingFailureException` instead of losing data — a
detectable conflict rather than silent corruption. One line, and the demo changes character
completely.

### 1.3 Extract appointment parsing into a tested domain class  ⏱ ~2h
Right now the parse/format/conflict logic is embedded and untestable. Pull it into
`AppointmentList` with pure methods (`parse`, `format`, `add`, `remove`, `overlaps`) and unit-test
it. Testable business logic is the clearest signal of engineering maturity in a seminar.

---

## Tier 2 — strong additions if time allows

### 2.1 `Appointment` as a first-class entity  ⏱ ~4h
Separate table, FK to user, indexed datetime column. Keep the custom-field path working so nothing
breaks; migrate behind a flag. Solves normalisation, queryability and per-appointment metadata.

### 2.2 OpenAPI / Swagger UI  ⏱ ~30min
`springdoc-openapi-starter-webmvc-ui` gives browsable API docs from existing annotations. Cheap and
it presents very well in a demo.

### 2.3 Integration tests with Testcontainers  ⏱ ~2h
Real PostgreSQL in tests rather than H2. Demonstrates you understand the difference.

---

## Tier 3 — only if everything else is done
- Rate limiting on auth endpoints
- Refresh tokens (currently access-token only)
- Android SDK instrumentation tests

---

## What NOT to do
- **Do not rewrite it.** It works. The examiner grades what runs.
- **Do not port Alma to Swift for the iOS course** — 2,849 lines and 25 dependencies. Ask the
  lecturer whether React Native is acceptable first.

---

## The framing that earns marks

Do not present the string-storage design as a mistake. Present it as a trade-off you understood:

> "Appointments are stored as a custom field to keep the SDK generic — no domain-specific entity in a
> general-purpose user library. The cost is normalisation and concurrent-update safety. I reproduced
> the lost-update failure with a test, then added optimistic locking. In production I would extract
> `Appointment` to its own table with a foreign key, an indexed datetime, and the same locking."

That sentence is worth more than any amount of additional code.
