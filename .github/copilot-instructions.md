# fp-tilgang

Centralized access-control service for Team Foreldrepenger backends.

## Shared context

- Source of truth for shared domain, architecture, and conventions: `navikt/fp-context`
- Copilot Space: `navikt/TeamForeldrepenger`

## Repo-specific context

| Topic        | Details                                                                                |
|--------------|----------------------------------------------------------------------------------------|
| Role         | Resolves saksbehandler roles, AD groups, and population checks for consuming services  |
| Consumers    | All Team Foreldrepenger backends and services requiring access control                 |
| Tech stack   | Standard fp Java backend                                                               |
| Integrations | Azure/Entra, PDL-PIP-API, Skjermet person                                              |
| Data         | No application database; Redis-backed caching                                          |

The core purpose is access control (an Xacml PIP). The app also provide information services used by `fp-los`, `fp-sak`, and `fp-mottak`

## Entry points

- `PopulasjonRestTjeneste`: Access control service - has requesting user access to persons by ids or case numbers  
- `AnsattInfoRestTjeneste`: Azure/Entra lookup for basic saksbehandler information and roles.
- `RutingRestTjeneste`: Get properties used by `fp-sak` and `fp-mottak` for case routing to Nav offices.

## Verification

- For integration impact, verify via `navikt/fp-autotest`.
- Most relevant suites: `verdikjede` and `fplos`.
