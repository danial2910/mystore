# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./mvnw spring-boot:run                          # Run the app (localhost:8080)
./mvnw test                                     # Run all tests
./mvnw test -Dtest=ItemsControllerTest          # Run a single test class
./mvnw test -Dtest=ItemsControllerTest#methodName  # Run a single test method
./mvnw clean verify                             # Build + test + JaCoCo coverage report
```

Spring profile for CI: `./mvnw ... -Dspring.profiles.active=jenkins`

## Architecture

**Package layout:**
- `com.store.controller` — HTTP handlers (AuthController, ItemsController, AdminController)
- `com.store.model` — JPA entities (`User`, `Item`) and form DTOs (`UserDTO`, `ItemDTO`)
- `com.store.service` — Spring Data JPA repositories (`UserRepository`, `ItemRepository`)
- `com.store.security` — `SecurityConfig`: BCrypt, form login, role-based route rules
- `com.store.config` — `DataInitializer` (seeds admin user), `GlobalModelAttributes` (injects `isAuthenticated`/`isAdmin` into every template), `WebConfig` (image static resource handler)

**Item lifecycle:** User creates → `PENDING` → Admin approves/rejects → `APPROVED`/`REJECTED`. Owner can cancel a `PENDING` or `APPROVED` item → `CANCELLED`. Only `APPROVED` items appear in the public `/items` list.

**Ownership enforcement:** `ItemRepository.findByIdAndOwner(id, user)` is used before any edit/delete to prevent users from touching other users' items.

**Image uploads:** `MultipartFile` in `ItemDTO`, saved with UUID filenames to `app.upload.dir` (`public/images/`), served via `/images/**` static mapping in `WebConfig`.

**Roles:** Stored as a comma-separated string on `User.roles` (e.g. `ROLE_USER,ROLE_ADMIN`). Admin routes (`/admin/**`) require `ROLE_ADMIN`. Default admin credentials come from `application.properties` (`app.admin.*`) and are seeded by `DataInitializer` on startup.

**Templates:** Thymeleaf under `src/main/resources/templates/` — `auth/`, `items/`, `admin/`, and `account.html`. Bootstrap 5 + Font Awesome 6. Global model attributes `${isAuthenticated}` and `${isAdmin}` are available in every template.

**Database:** PostgreSQL via Supabase. Connection configured in `application.properties`; `application-jenkins.properties` overrides for the Docker-based CI environment. Hibernate DDL is set to `update` (auto-migrates schema).

**Tests:** Unit tests only — Mockito mocks for repositories and security context, no database. `@ExtendWith(MockitoExtension.class)` + AssertJ assertions throughout.
