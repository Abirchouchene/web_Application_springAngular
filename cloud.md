# cloud.md — Project Memory

> Primary context file. Check here before reading any other file.
> Updated: 2026-04-27

---

## 1. System Architecture

| Service | Port | Root path | Location |
|---|---|---|---|
| `callcenter-service` | 8082 | `/api` | `c:/Users/Abir/callcenter-service/` |
| `contact-service` | 8081 | `/api` | `c:/Users/Abir/contact-service/` |
| Angular frontend | 4200 | — | `c:/Users/Abir/Desktop/test/frontend/` |
| Keycloak | 8080 | — | `http://192.168.10.161:8080/` |
| MySQL | 3306 | — | `192.168.10.161:3306` (DB: `call-center`) |
| MinIO | 9000 | — | `http://192.168.10.161:9000` |

> ⚠️ Angular frontend is at `c:/Users/Abir/Desktop/test/frontend/` — NOT `c:/Users/Abir/frontend/`
> ⚠️ `c:/Users/Abir/frontend/` is a STALE COPY of backend code — never edit it.

---

## 2. Keycloak Configuration

- **Realm**: `Portal`
- **Admin client ID**: `call-center-backend` (with dash — never `callcenter-backend`)
- **Client secret**: `lGNpcgyHlDiFcubxhQo9nxeItxkl6Rgr`
- **Grant type**: `client_credentials`
- **Frontend client ID**: `uptech-rest-api`
- **Config bean**: `Config/KeyCloakConfig.java`
  - Strips `/realms/xxx` from `keycloak.auth-server-url` to get admin base URL
  - Profile guard: `@Profile("!dev-local")`
- **Roles (realm)**: `admin`, `manager`, `agent`, `demandeur`
- **Groups**: `Admins`, `Managers`, `Agents`, `Demandeurs`
- **JWT principal attribute**: `sub` (set in `application.properties`)
- **Username claim used in controllers**: `preferred_username` → `userRepository.findByUsername()`

---

## 3. Key Entities & DB Tables

### callcenter-service entities (`com.example.callcenter.Entity`)

| Entity | Table | Notes |
|---|---|---|
| `User` | `user` | id_user PK, role enum |
| `Request` | `request` | title TEXT, description TEXT (altered by DbSchemaMigration) |
| `Report` | `report` | FK → request (owning side: request.report_id) |
| `RequestContactStatus` | `request_contact_status` | per-request-per-contact status |
| `EvaluationRecord` | `evaluation_record` | quality eval form submissions (ratings JSON, binary answers JSON) |
| `Feedback` | `feedback` | requester star rating + comment per report (nullable user_id) |
| `Notification` | `notification` | — |
| `PasswordResetToken` | `password_reset_token` | — |
| `Logs` | `logs` | action log entries |

**Role enum**: `ADMIN`, `MANAGER`, `AGENT`, `SURVEY_REQUESTER`

**Dual-status system**:
- `Contact.callStatus` — global status on the contact
- `RequestContactStatus.status` — per-request-per-contact status
- Both must be updated together when an agent changes a call status

---

## 4. Java Coding Rules (Eclipse / Java 8 compliance)

All new `.java` files are compiled with Java 8 compliance by Eclipse. These patterns MUST be used:

| Avoid | Use instead |
|---|---|
| `str.isBlank()` | `str.trim().isEmpty()` |
| `List.of(a, b)` | `new ArrayList<>()` + `.add()` |
| `Map.of(k, v)` | `new LinkedHashMap<>()` + `.put()` |
| `switch (x) { case A -> ... }` | `switch (x) { case A: ... break; }` |
| Text blocks `"""..."""` | `String.format(loadedTemplate, args)` |

**Prompts and HTML templates**: never hardcode in Java source. Use:
- `@Value("classpath:prompts/xxx.txt") Resource promptResource;`
- `@PostConstruct void loadPrompt()` with `FileCopyUtils.copyToByteArray()`
- Store in `src/main/resources/prompts/` and `src/main/resources/templates/email/`

---

## 5. Service Layer — Key Files

### `Service/EmailService.java`
- Loads 4 HTML templates at startup via `@PostConstruct` from `resources/templates/email/`
- `buildResetPasswordHtml()` → `String.format(resetPasswordTemplate, name, resetLink)`
- `buildWelcomeHtml()` → `String.format(welcomeTemplate, name, username, tempPassword)`
- `buildReportApprovalHtml()` → `String.format(reportApprovalTemplate, 10 args)`

### `Service/ReportService.java`
- `generateReport(requestId)`: sets BOTH sides of the FK — `report.setRequest(request)` AND `request.setReport(report)`
- `generatePdf(reportId)`: builds PDF, uploads to MinIO, falls back to local file
- `toReportDTO()`: null-safe — returns `0`/`0.0` for null `totalContacts`, `contactedContacts`, `contactRate`
- `DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")` — used only for PDF internal formatting (NOT in JSON responses)

### `Service/QualityEvaluationService.java`
- `generateForm(reportId)`: returns contextual `EvaluationForm` (ratings + binary questions + open question)
  - If no linked request → `generateDefault()`
  - If OpenAI enabled → `generateWithAI()`
  - Fallback → `generateRuleBased()`
- `processSubmission(reportId, userId, submission)`: upserts `EvaluationRecord` (one per user per report)

### `Service/FeedbackService.java`
- `submitFeedback(reportId, rating, comment, userId)`: upserts `Feedback` record
  - If `userId` is null → creates new record (no upsert lookup)
  - If OpenAI enabled → calls `callOpenAiAnalysis()` and stores result
- `getFeedbackByReport(reportId)`: list all feedback for a report
- `getFeedbackByReportAndUser(reportId, userId)`: get current user's feedback
- Prompt loaded from `resources/prompts/feedback-analysis-prompt.txt`

### `Service/SurveyAssistantService.java`
- Per-survey AI assistant scoped to a specific request's data
- `askByReportId(reportId, req, username)`: resolves request via `RequestRepository.findByReport_Id()`
- Ownership check: requester must own the survey, or be ADMIN/MANAGER → throws `AccessDeniedException`
- Builds context JSON: contacts, responses by question, report summary
- Rule-based fallback with 12 intent categories (French)
- Prompt loaded from `resources/prompts/survey-assistant-prompt.txt`

### `Service/DashboardService.java`
- `buildRecentActivity()`: timestamps formatted as ISO 8601 (`yyyy-MM-dd'T'HH:mm:ss`) for Angular DatePipe
- `buildDailyTrend()`: dates formatted as `dd/MM` — this is for chart labels only (not DatePipe)

### `Service/AdminService.java`
- `createUser()`: creates KC user first (fail-fast), then local DB, sends welcome email
- `changeUserRole()`: updates DB + KC role/group + calls `logout()` to invalidate KC sessions
- `syncKeycloakUsers()`: syncs all KC users into local DB

---

## 6. Controller Endpoints

### `Controller/ReportController.java` — `/reports`

| Method | Path | Description |
|---|---|---|
| POST | `/generate/{requestId}` | Generate report for a request |
| GET | `/{reportId}` | Get report by ID |
| GET | `/request/{requestId}` | Get report by request |
| GET | `/request/{requestId}/status` | Get report generation status |
| GET | `/{reportId}/pdf` | Download PDF |
| GET | `/{reportId}/download-url` | Get MinIO presigned URL (fallback: local) |
| POST | `/{reportId}/approve` | Approve report |
| POST | `/{reportId}/reject` | Reject report |
| POST | `/auto-generate` | Trigger scheduled auto-generation |
| GET | `/{reportId}/quality-evaluation` | Get contextual eval form |
| POST | `/{reportId}/quality-evaluation` | Submit evaluation → saves EvaluationRecord |

### `Controller/FeedbackController.java` — `/reports`

| Method | Path | Description |
|---|---|---|
| POST | `/{reportId}/feedback` | Submit/update feedback (userId from JWT) |
| GET | `/{reportId}/feedback` | List all feedback for report |
| GET | `/{reportId}/feedback/mine` | Get current user's feedback |
| POST | `/{reportId}/feedback/{feedbackId}/analyze` | Trigger AI analysis |

### `Controller/AiChatController.java` — `/ai-chat`

| Method | Path | Description |
|---|---|---|
| POST | `/message` | Global dashboard AI chat |
| POST | `/survey/{requestId}/message` | Per-survey assistant (by requestId) |
| POST | `/report/{reportId}/message` | Per-survey assistant (by reportId) |

### `Controller/AuthController.java` — `/admin`

| Method | Path | Description |
|---|---|---|
| GET | `/user/profile` | Get current user's profile from DB |
| PUT | `/user/profile` | Update fullName + email |

---

## 7. DTOs

| DTO | Location | Key fields |
|---|---|---|
| `ReportDTO` | `DTO/ReportDTO.java` | id, requestTitle, totalContacts(0 if null), contactedContacts(0 if null), contactRate(0.0 if null) |
| `QualityEvaluationDTO` | `DTO/QualityEvaluationDTO.java` | `EvaluationForm`, `EvaluationSubmission`, `EvaluationResult` (has `recordId`) |
| `FeedbackDTO` | `DTO/FeedbackDTO.java` | id, reportId, reportTitle, userId, rating, comment, submittedAt, aiAnalysis, aiAnalyzed |
| `UpdateProfileDTO` | `DTO/UpdateProfileDTO.java` | fullName, email |
| `DashboardDTO` | `DTO/DashboardDTO.java` | KPIs, maps, lists, `RecentActivityDTO` (timestamp as ISO string) |

---

## 8. Repositories

| Repository | Key custom methods |
|---|---|
| `RequestRepository` | `findByReport_Id(Long)`, `findCompletedRequestsWithoutReport()` |
| `EvaluationRecordRepository` | `findByReport_Id`, `findByReport_IdAndUserId` |
| `FeedbackRepository` | `findByReport_Id`, `findByReport_IdAndUserId`, `existsByReport_IdAndUserId` |
| `UserRepository` | `findByUsername(String)` |

---

## 9. Application Properties (key entries)

```properties
server.port=8082
spring.datasource.url=jdbc:mysql://192.168.10.161:3306/call-center
spring.jpa.hibernate.ddl-auto=update

# Jackson — serialize LocalDateTime as ISO-8601 strings (not arrays)
spring.jackson.serialization.write-dates-as-timestamps=false

# Eureka — disabled (no registry running)
eureka.client.enabled=false
logging.level.com.netflix.discovery=ERROR
logging.level.com.netflix.eureka=ERROR

# OpenAI — disabled by default
openai.api-key=${OPENAI_API_KEY:}
openai.enabled=${OPENAI_ENABLED:false}
openai.model=gpt-4
```

---

## 10. Angular Frontend — `c:/Users/Abir/Desktop/test/frontend/`

### Environment (`src/environments/environment.ts`)
```typescript
gatewayUrl: 'http://localhost:8082'
apiUrl: 'http://localhost:8082/api'
contactApiUrl: 'http://localhost:8082/api/contacts'
authUrl: 'http://localhost:8082/api/auth'
adminUrl: 'http://localhost:8082/api/admin'
userUrl: 'http://localhost:8082/api/users'
wsUrl: 'ws://localhost:8082/ws'
```

### Key Services

| Service | File | Key methods |
|---|---|---|
| `DashboardService` | `services/apps/dashboard.service.ts` | `getStats()` |
| `ReportService` | `services/apps/report.service.ts` | `generateReport`, `approveReport`, `rejectReport`, `getReportStatus`, `generateReportPdf`, `getQualityEvaluationForm`, `submitQualityEvaluation`, `submitFeedback`, `getMyFeedback` |
| `RequestService` | `services/apps/ticket/request.service.ts` | full CRUD for requests |

### Key Components

| Component | Path | Notes |
|---|---|---|
| Dashboard | `pages/dashboards/dashboard1/` | ApexCharts — all series guarded against empty arrays |
| ReportList | `pages/apps/Reports/report-list/` | "Donner mon avis" button → opens inline feedback overlay dialog |
| ReportDetails | `pages/apps/Reports/report-details/` | Dialog showing report stats |
| TicketDetails | `pages/apps/Requests/TicketDetails/` | Agent view of a request |
| RequestManagerView | `pages/apps/RequestManager/request-manager-view/` | Manager assigns agent |

---

## 11. Important Fixes Applied

### Fixes from 2026-04-27 session

| Bug | Fix |
|---|---|
| `DatePipe` error — `'Unable to convert "24/04/2026 18:27" into a date'` | `DashboardService.buildRecentActivity()` changed formatter to ISO 8601 `yyyy-MM-dd'T'HH:mm:ss`. Added `spring.jackson.serialization.write-dates-as-timestamps=false` to serialize all `LocalDateTime` DTOs as ISO strings. |
| ApexCharts `translate(NaN, 0)` | `dashboard1.component.ts`: all chart series guarded — `statusChart.series` was `[]` when no data → now `[1]` with label "Aucune donnée". Same fix for `priorityChart`, `categoryChart`, `trendChart`. |
| Feedback not saved — backend returned 401 | `FeedbackController.submitFeedback()` was calling `resolveUserId()` and returning 401 when JWT user not found in DB. Removed early 401 return. `Feedback.userId` column made nullable. `FeedbackService` skips upsert lookup when `userId` is null. |
| Feedback form missing in frontend | `ReportService` had no feedback methods. Added `submitFeedback()`, `getMyFeedback()`. Added `FormsModule` + `ReportService` to `ReportListComponent`. Added "Donner mon avis" menu item + full overlay dialog (⭐ stars + textarea + send button) to `report-list.component.html`. |
| Quality evaluation 404 | Caused by `QualityEvaluationService` using `isBlank()` + `List.of()` → Eclipse Java 8 compile failure → bean not created → controller constructor failure. Fixed: `isBlank()` → `trim().isEmpty()`, `List.of()` → `new ArrayList<>() + .add()`. |
| `EvaluationRecord` not stored | `processSubmission` had wrong signature in controller call. Fixed controller to pass `userId` from JWT. |
| Eureka heartbeat ERROR logs every 30s | `eureka.client.enabled=false` + `logging.level.com.netflix.*=ERROR` |

### Fixes from earlier sessions (summarized)

| Bug | Fix |
|---|---|
| Call status not persisted | `forkJoin` updates both `RequestContactStatus` and `Contact.callStatus` |
| Requests attributed to user id=1 | `submitRequest()` derives user from JWT `preferred_username`, ignores client-supplied userId |
| Agent list 3-min load | `convertToResponseDTO(request, boolean loadContacts)` — list endpoints skip contact loading |
| Report AI tab always empty | Frontend now calls `GET /reports/{id}/ai-insights` on-demand when `aiInsightsData` is null |
| `request.report_id` FK null | `generateReport()` sets both sides of FK; `DbSchemaMigration.repairOrphanReportLinks()` repairs historical rows |
| Contact Feign fails (Eureka down) | `@FeignClient(url="${contact.service.url}")` bypasses Eureka |

---

## 12. Prompts & Templates

```
src/main/resources/
  prompts/
    quality-evaluation-prompt.txt    ← QualityEvaluationService (AI form generation)
    feedback-analysis-prompt.txt     ← FeedbackService (AI feedback analysis)
    survey-assistant-prompt.txt      ← SurveyAssistantService (per-survey chat)
    ai-report-prompt.txt             ← ReportService (AI report insights)
    call-copilot-prompt.txt          ← CallCopilotService
    consistency-check-prompt.txt     ← ConsistencyService
  templates/email/
    reset-password.html
    welcome.html
    report-approval.html
    stats-section.html
```

---

## 13. Build & Restart

```bash
# Kill process on port 8082
netstat -ano | findstr :8082   # find PID (Windows)
taskkill /F /PID <PID>

# Build & start callcenter-service
cd c:/Users/Abir/callcenter-service
mvn package -DskipTests
java -jar target/callcenter-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Wait for startup
until grep -q "Started CallCenter" app.log; do sleep 3; done
```

> ⚠️ JAR file is locked while running — must kill before rebuilding.
> ⚠️ MySQL has low `max_connections` — wait ~30s after kill before restart.
> ⚠️ Use PowerShell `Get-NetTCPConnection -LocalPort 8082` to find PID if `netstat` shows PID 0.

---

## 14. Mailjet Configuration

Real API keys in `application.properties` lines 67-70.
Sender: `noreply@uptech.com.tn` / `NO-REPLY`

---

## 15. Role Mapping

| App Role | Keycloak Realm Role | Keycloak Group |
|---|---|---|
| ADMIN | admin | Admins |
| MANAGER | manager | Managers |
| AGENT | agent | Agents |
| SURVEY_REQUESTER | demandeur | Demandeurs |
