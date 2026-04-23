# cloud.md — Project Memory

> Primary context file. Check here before reading any other file.
> Updated: 2026-04-23

---

## 1. System Architecture

| Service | Port | Root path | Location |
|---|---|---|---|
| `callcenter-service` | 8082 | `/api` | `c:/Users/Abir/callcenter-service/` |
| `contact-service` | 8081 | `/api` | `c:/Users/Abir/contact-service/` |
| Angular frontend | 4200 | — | `c:/Users/Abir/frontend/` |
| Keycloak | 8080 | — | `http://192.168.10.161:8080/` |
| MySQL | 3306 | — | `192.168.10.161:3306` (DB: `call-center`) |
| MinIO | 9000 | — | `http://192.168.10.161:9000` |

> ⚠️ `c:/Users/Abir/frontend/src/main/java/...` is a STALE COPY of backend code — never edit it. The real contact-service is at `c:/Users/Abir/contact-service/`.

---

## 2. Keycloak Configuration

- **Realm**: `Portal`
- **Admin client ID**: `callcenter-backend`
- **Client secret**: `lGNpcgyHlDiFcubxhQo9nxeItxkl6Rgr`
- **Grant type**: `client_credentials`
- **Frontend client ID**: `uptech-rest-api`
- **Config bean**: `c:/Users/Abir/callcenter-service/src/main/java/com/example/callcenter/Config/KeyCloakConfig.java`
  - Strips `/realms/xxx` from `keycloak.auth-server-url` to get admin base URL
  - Profile guard: `@Profile("!dev-local")`
- **Roles (realm)**: `admin`, `manager`, `agent`, `demandeur`
- **Groups**: `Admins`, `Managers`, `Agents`, `Demandeurs`

---

## 3. Key Entities & Packages

### callcenter-service (`com.example.callcenter`)

| Layer | Package |
|---|---|
| Controllers | `Controller/` |
| Services | `Service/` |
| Entities | `Entity/` |
| DTOs | `DTO/` |
| Repositories | `Repository/` |
| Config | `Config/` |

**Key entities**: `User`, `Role` (enum: ADMIN, MANAGER, AGENT, SURVEY_REQUESTER), `Request`, `Report`, `Notification`, `RequestContactStatus`, `PasswordResetToken`

**Dual-status system**:
- `Contact.callStatus` — global status on the contact
- `RequestContactStatus.status` — per-request-per-contact status (read by `RequestController.convertToResponseDTO()`)
- Both must be updated together when an agent changes a call status in the workflow

### contact-service (`com.example.contactservice`)

| Layer | Class |
|---|---|
| Controller | `ContactController.java` |
| Service | `ContactService.java` |
| Entities | `Contact`, `Tag`, `ContactStatus` |
| Repositories | `ContactRepository`, `TagRepository` |

---

## 4. File Summaries

### `callcenter-service/src/.../Service/AdminService.java`
- Full Keycloak integration: create/delete/role-change/sync users
- `createUser(dto)`: creates Keycloak user first (fail-fast), then local DB, then sends welcome email
- Password set as **non-temporary** (`cred.setTemporary(false)`) — users can login immediately
- Email verified set to **true** — no verification step
- Keycloak errors now throw (not silently swallowed) — 409 = duplicate user message
- `syncKeycloakUsers()`: syncs all KC users into local DB
- `changeUserRole()`: updates local DB + removes old KC role/group + assigns new KC role/group

### `callcenter-service/src/.../Controller/AdminController.java`
- `POST /admin/users` → `adminService.createUser(dto)` → returns `{ "error": msg }` on failure
- All endpoints wrapped in try-catch returning `Map.of("error", e.getMessage())`
- Endpoints: CRUD users, toggle-enabled, send-reset-email, reset-password, forgot-password, reset-password-token, sync, change-role

### `callcenter-service/src/.../DTO/CreateUserDTO.java`
- Fields: `username`, `email`, `firstName`, `lastName`, `password`, `role` (Role enum)

### `callcenter-service/src/main/resources/application.properties`
- MySQL, Keycloak, MinIO, Mailjet, RabbitMQ, OpenAI config
- `app.frontend-url=http://localhost:4200`
- Mailjet keys are placeholder (`YOUR_MAILJET_API_KEY`) — email may not work

### `callcenter-service/src/main/resources/application-dev-local.properties`
- Uses H2 in-memory DB, disables Keycloak OAuth2 auto-config
- Start with: `mvn spring-boot:run -Dspring-boot.run.profiles=dev-local`

### `contact-service/src/.../controller/ContactController.java`
- CRUD contacts + tag management endpoints
- Debug endpoints: `GET /debug/tag-contacts`, `POST /debug/fix-pk`
- `JdbcTemplate jdbc` injected for debug queries

### `contact-service/src/.../service/ContactService.java`
- `deleteTag()`: detaches tag from all contacts before deleting (avoids FK violation)
- `createContact()`, `updateContact()`: handle tag assignment via `tagIds`
- `updateContactCallStatus()`: updates `callStatus`, `callNote`, `lastCallAttempt`

---

## 5. Frontend Key Files

### Angular services
| Service | File | Base URL |
|---|---|---|
| ContactService | `src/app/services/apps/contact/contact.service.ts` | `environment.contactApiUrl` |
| UserService | `src/app/services/apps/user/user.service.ts` | `environment.adminUrl` |
| RequestService | `src/app/services/apps/ticket/request.service.ts` | `environment.apiUrl` |

### Key components
| Component | Path | Notes |
|---|---|---|
| ContactManagement | `pages/apps/contact/contact-management/` | Tag CRUD at bottom of page |
| AddUserDialog | `pages/apps/user-management/add-user-dialog.component.ts` | Inline template, creates/edits users |
| AddRequestDialog | `pages/apps/invoice/add-invoice/add-request.component.*` | Multi-step form; `trackByIndex` fixes focus loss on option inputs |
| EditRequestDialog | `pages/apps/invoice/edit-request-dialog/` | Modal dialog (not page) with transfer panel for contacts |
| CallsComponent | `pages/apps/Calls/calls.component.ts` | Uses `forkJoin` to update both per-request and global contact status |
| InvoiceList | `pages/apps/invoice/invoice-list/` | Edit button opens `EditRequestDialogComponent` as modal |

### `add-user-dialog.component.ts`
- Form: `username`, `email`, `firstName`, `lastName`, `password`, `role`
- `onSubmit()` sends full payload to `userService.createUser()`
- Error display: reads `err?.error?.message || err?.error?.error` from HTTP error

---

## 6. Important Fixes Applied (this session)

| Bug/Feature | Fix |
|---|---|
| Call status not persisted to DB | `forkJoin` in `calls.component.ts` now updates both `RequestContactStatus` and global `Contact.callStatus` |
| Option input focus loss in add-request form | `trackByIndex` on `*ngFor` + `(keydown.enter)="$event.preventDefault()"` |
| "Modifier la demande" as full page | Replaced with `EditRequestDialogComponent` modal dialog |
| Tag delete 404 | Was editing stale frontend copy; fixed real `contact-service` at `c:/Users/Abir/contact-service/` |
| Keycloak `invalid_client` 401 | Client ID in `application.properties` was `callcenter-backend`, real Keycloak client name is `call-center-backend` (with dash) |
| Keycloak trailing whitespace | Client secret had trailing space in properties file — stripped |
| Email verification on user creation | Keycloak user now created with `emailVerified=false` + required action `VERIFY_EMAIL`; `executeActionsEmail` called after creation → Keycloak auto-redirects user to verification page on first login |
| Rich approval notification UI | Backend `approveReport()` returns `Map<String,Object>` with recipient info; frontend opens `ReportApprovalResultDialogComponent` instead of snackbar |
| Role change not reflected in Keycloak at auth time | `changeUserRole()` now calls `getUsersResource().get(kcUserId).logout()` after role/group update — invalidates all active KC sessions so next login gets a fresh JWT with the new role |
| Add-user KC sync failing on 409 (user silently not synced) | `createKeycloakUser()` on HTTP 409: if KC has a user with the SAME USERNAME, reuse that kcId to sync role/group; if collision is email-only (different username), throw "L'email est déjà utilisé par un autre utilisateur Keycloak" so admin picks a different email |
| Frontend 404 on `GET /api/callbacks/upcoming/{agentId}` | `CallbackController` lives in **contact-service** (port 8081), not callcenter-service. Added `environment.callbackApiUrl='http://localhost:8081/api/callbacks'` and switched `callback.service.ts` to use it. Follows existing pattern where contacts also call 8081 directly via `contactApiUrl`. |
| 500 on `POST /requests/submit` — "Data too long for column 'description'" | `description` and `note` columns in `request` table were VARCHAR(255). Annotated fields with `@Column(columnDefinition="TEXT")` in [Request.java](src/main/java/com/example/callcenter/Entity/Request.java). Hibernate `ddl-auto=update` doesn't alter existing VARCHAR→TEXT — added [DbSchemaMigration.java](src/main/java/com/example/callcenter/Config/DbSchemaMigration.java) `CommandLineRunner` that runs `ALTER TABLE` via `JdbcTemplate` on startup (idempotent: checks `INFORMATION_SCHEMA.COLUMNS` first). |
| Submitted requests always attributed to "abir" (user id=1) regardless of logged-in user | `add-request.component.ts` had `userId: [environment.callCenterSubmitUserId, ...]` (hardcoded `1`). Now injects `RoleService`, reads current user id from `/user/me` via `getUserInfoSnapshot()?.id`, and patches form in `ngOnInit` subscription to handle async load. |
| Agent's request list showed nothing / wrong requests | `invoice-list.component.ts` called `getAssignedRequests(1)` (hardcoded). Now uses `roleService.getUserInfo()` to drive routing: `AGENT`→`getAssignedRequests(currentUserId)`, `SURVEY_REQUESTER`→`getRequestsByUserId(currentUserId)` (scoped to their own), others→`getAllRequests()`. |
| All requests showed "abir" as demandeur in details view | Hardened [RequestController.java](src/main/java/com/example/callcenter/Controller/RequestController.java) `submitRequest()`: now **always derives the requester from the JWT's `preferred_username`**, overrides `requestDTO.userId` with the DB id matched from the authenticated user. Any client-supplied userId is ignored — so even if the frontend is buggy or malicious, the server attributes the request correctly. **Note**: existing requests stored with `user_id=1` cannot be retroactively reassigned — only requests submitted after this fix will carry the correct user. |
| Request details page: contact names + phone null | `ContactClient` Feign was `@FeignClient(name="contact-service")` — resolved via Eureka, but Eureka isn't running (port 8761 connection refused → `UnknownHostException: contact-service`). Fix: added `url = "${contact.service.url:http://localhost:8081}"` on the `@FeignClient` annotation, and property `contact.service.url=http://localhost:8081` in `application.properties`. Now hits contact-service directly, bypassing service discovery. |
| Manager approval list took ~3 min to load | `convertToResponseDTO()` was doing a Feign call to contact-service **per contact per request** for every list endpoint. 49 requests × ~5 contacts each = ~250 blocking HTTP calls. Fix: added `convertToResponseDTO(Request, boolean loadContacts)` overload; list endpoints (`/All`, `/user/{id}`, `/type/{t}`, `/assigned/{agentId}`) now call with `loadContacts=false` and return empty `contacts[]`. Single-item details endpoint (`/requests/{id}`) still loads full contact details. Dropped `/All` latency from ~3 min to ~4.3 s. |
| Agent "Appeler" → contacts missing in calls workflow | Side-effect of the list-speedup above: `/assigned/{agentId}` now returns requests with empty `contacts[]`, so `calls.component.ts buildCallEntries()` had nothing to iterate. Fix: `openDetailView()` now calls `requestService.getRequestById(req.idR)` to fetch the full request (with contacts) on click, replaces the row in `assignedRequests`, then rebuilds call entries. List stays fast; contacts load on-demand when an agent drills in. |
| Report details "Analyse IA" tab always showed "Aucune analyse disponible" | Frontend only rendered `report.aiInsightsData` if pre-populated in DB — never called the `GET /reports/{id}/ai-insights` endpoint (which generates on demand + caches). Fix: added `getAiInsights()` and `generateAiInsights()` to `report.service.ts`; `parseAiInsights()` in `report-details.component.ts` now falls back to `loadAiInsights()` when `aiInsightsData` is null. Added loading spinner and "Régénérer l'analyse" button. Backend endpoint uses rule-based fallback if OpenAI is disabled (`openai.enabled=false`). |

## 7. Mailjet Configuration

Real API keys are set in `application.properties` lines 61-62 (`api-key` + `secret-key`).
Login to Mailjet dashboard is `upskills@uptech.com.tn` / `p#7ykH5H6*LkSnS` — NOT used by the app; only for Mailjet web UI access.
Sender: `noreply@callflow.com` / `CallFlow`

## 8. Keycloak Client Config (Realm: Portal)

- Client ID: **`call-center-backend`** (with dash — do NOT write `callcenter-backend`)
- Service account roles assigned: `manage-users`, `view-users`, `view-realm`, `query-groups` (all from `realm-management` client)
- Required for admin-level operations via `AdminService.java`

---

## 9. Build & Restart Commands

```bash
# callcenter-service
cd c:/Users/Abir/callcenter-service
./mvnw.cmd clean package -DskipTests
# Kill old process
netstat -ano | grep :8082   # find PID
taskkill //F //PID <PID>
# Start
java -jar target/callcenter-0.0.1-SNAPSHOT.jar > /tmp/callcenter.log 2>&1 &

# contact-service
cd c:/Users/Abir/contact-service
./mvnw.cmd clean package -DskipTests
netstat -ano | grep :8081   # find PID
taskkill //F //PID <PID>
java -jar target/contact-service-0.0.1-SNAPSHOT.jar > /tmp/contact.log 2>&1 &
```

> ⚠️ Use `taskkill //F //PID` (double-slash) in bash. Single-slash parses as flags.
> ⚠️ MySQL has a low `max_connections` — killing java processes with `//F` leaves dangling connections. Wait ~30s before restarting.

---

## 10. Role Mapping

| App Role | Keycloak Realm Role | Keycloak Group |
|---|---|---|
| ADMIN | admin | Admins |
| MANAGER | manager | Managers |
| AGENT | agent | Agents |
| SURVEY_REQUESTER | demandeur | Demandeurs |
