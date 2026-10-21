# CareBridge AI — Jira Project Plan

Project key suggestion: **CBA**. Board type: Scrum, 2-week sprints, 5 sprints total
(matches the 10-week roadmap from the original architecture doc).

## Epics

| Epic | Key | Description |
|---|---|---|
| Platform Foundation | CBA-E1 | Eureka, Gateway, Auth, CI/CD skeleton |
| Core Domain Services | CBA-E2 | Patient, Provider, Appointment, Notification services |
| Agentic AI Layer | CBA-E3 | Gemini integration, LangGraph-style orchestration, tool-calling |
| Frontend Experience | CBA-E4 | React + Tailwind app, all 9 pages, auth flow |
| Production Readiness | CBA-E5 | Testing, Docker, CI/CD, documentation, demo prep |

---

## Sprint 1 — Platform Foundation (Weeks 1–2)

**Sprint goal:** A patient can register, log in, and receive a valid JWT, routed through
the Gateway, with Eureka showing live service registration.

| Story | Points | Tasks |
|---|---|---|
| **CBA-101** As a developer, I want a shared library so every service can validate JWTs consistently | 3 | Build `common-lib` (JwtUtil, Role, ApiResponse, exception handler) |
| **CBA-102** As a platform operator, I want services to discover each other automatically | 2 | Stand up Eureka Server; verify registration |
| **CBA-103** As a client app, I want a single entry point for all API traffic | 5 | Build API Gateway; routing config; reactive JWT filter |
| **CBA-104** As a user, I want to register an account with a role (Patient/Doctor) | 5 | Auth Service: User model, register endpoint, BCrypt hashing |
| **CBA-105** As a user, I want to log in and receive a token | 3 | Auth Service: login endpoint, JWT issuance, refresh flow |
| **CBA-106** As a developer, I want CI to catch build breaks early | 3 | Initial GitHub Actions workflow (build + test on PR) |

**Sprint 1 total: 21 points**

---

## Sprint 2 — Core Domain Services (Weeks 3–4)

**Sprint goal:** A patient can complete a profile, and a doctor can list themselves with
availability; appointments can be booked manually end-to-end (no AI yet) — proves the
SOA plumbing before layering in agentic behavior.

| Story | Points | Tasks |
|---|---|---|
| **CBA-201** As a patient, I want to create and update my health profile | 5 | Patient Service: model, CRUD, validation |
| **CBA-202** As a patient, I want to log my symptoms over time | 3 | Symptom log model + endpoints |
| **CBA-203** As a doctor, I want to create my provider profile | 3 | Provider Service: Doctor model, CRUD |
| **CBA-204** As a doctor, I want to publish available appointment slots | 3 | AvailabilitySlot model + endpoints |
| **CBA-205** As a patient, I want to search doctors by specialty and city | 3 | Doctor search endpoint |
| **CBA-206** As a patient, I want to book an appointment with a doctor | 5 | Appointment Service: booking flow, Feign call to Provider Service to mark slot booked |
| **CBA-207** As a patient, I want a confirmation when my appointment is booked | 3 | Notification Service: mock dispatch, Feign call from Appointment Service |

**Sprint 2 total: 25 points**

---

## Sprint 3 — Agentic AI Layer (Weeks 5–7) — *Hackathon-ready checkpoint*

**Sprint goal:** A patient can describe symptoms in natural language and the agent
autonomously searches doctors, gets confirmation, and books the appointment — the core
demo flow.

| Story | Points | Tasks |
|---|---|---|
| **CBA-301** As a developer, I want a reliable Gemini client for reasoning | 5 | `GeminiClient`: text + JSON-mode generation, error fallback |
| **CBA-302** As the agent, I want to understand what the patient is asking for | 5 | `IntentExtractionNode` |
| **CBA-303** As a patient, I want the system to recognize a medical emergency | 8 | `RiskAssessmentNode` + `EmergencyEscalationNode`, keyword + LLM hybrid check |
| **CBA-304** As the agent, I want the patient's medical history before deciding | 3 | `ContextGatheringNode`, Feign client to Patient Service |
| **CBA-305** As the agent, I want to decide the right next action | 5 | `DecisionNode` |
| **CBA-306** As the agent, I want to search real doctors as a tool call | 5 | `DoctorSearchToolNode`, Feign client to Provider Service |
| **CBA-307** As a patient, I want to confirm a doctor before anything is booked | 5 | `SelectionParsingNode` (human-in-the-loop checkpoint) |
| **CBA-308** As the agent, I want to book the confirmed appointment autonomously | 5 | `ActionExecutionNode`, Feign client to Appointment Service |
| **CBA-309** As the agent, I want to generate referrals when appropriate | 3 | `ReferralToolNode` |
| **CBA-310** As a patient, I want a natural, clear final reply | 3 | `ResponseSynthesisNode` |
| **CBA-311** As an admin, I want to audit every autonomous action the agent took | 5 | `AgentActionLog`, audit endpoint |
| **CBA-312** As a developer, I want the graph's routing logic centralized and testable | 3 | `AgentGraphExecutor` + unit tests |

**Sprint 3 total: 50 points** *(largest sprint — matches this being the technical centerpiece)*

---

## Sprint 4 — Frontend Experience (Weeks 7–9, overlaps Sprint 3 tail)

**Sprint goal:** Full patient journey usable end-to-end in the browser, connected to the
live Gateway.

| Story | Points | Tasks |
|---|---|---|
| **CBA-401** As a visitor, I want to understand the product before signing up | 2 | Landing Page |
| **CBA-402** As a user, I want to register and log in | 5 | Register/Login pages, AuthContext, JWT persistence + refresh |
| **CBA-403** As a patient, I want a home base showing my care status | 3 | Patient Dashboard |
| **CBA-404** As a patient, I want to talk to the AI assistant | 8 | `AgentChatWindow`, live status indicator, confirmation option cards |
| **CBA-405** As a patient, I want to browse doctors manually too | 3 | Recommendations/search page |
| **CBA-406** As a patient, I want to see all my appointments | 3 | Appointments page |
| **CBA-407** As a patient, I want to track my referrals | 3 | Referral tracking timeline page |
| **CBA-408** As a patient, I want to see notifications sent to me | 2 | Notifications page |
| **CBA-409** As an admin, I want to inspect agent audit logs | 5 | Admin Dashboard + audit log lookup |
| **CBA-410** As a developer, I want routes protected by role | 3 | `ProtectedRoute`, role-based navigation |

**Sprint 4 total: 37 points**

---

## Sprint 5 — Production Readiness & Demo Prep (Week 10)

**Sprint goal:** The whole system is containerized, tested, documented, and pitch-ready.

| Story | Points | Tasks |
|---|---|---|
| **CBA-501** As a developer, I want automated tests for critical logic | 8 | Unit tests (Auth, Provider, RiskAssessmentNode), integration test (AuthController), graph routing tests |
| **CBA-502** As a QA engineer, I want a reusable API test suite | 3 | Postman collection covering full flow + negative/RBAC cases |
| **CBA-503** As a developer, I want one-command local deployment | 5 | Dockerfiles (all 8 services + frontend), unified `docker-compose.yml` |
| **CBA-504** As a developer, I want CI to build and test on every PR | 5 | Full GitHub Actions pipeline: backend build/test, frontend build/test, per-service Docker build matrix |
| **CBA-505** As a team, we want a compelling hackathon pitch | 3 | Architecture explainer, novelty statement, SDG alignment, demo script |

**Sprint 5 total: 24 points**

---

## Velocity Summary

| Sprint | Points | Theme |
|---|---|---|
| 1 | 21 | Foundation |
| 2 | 25 | Core services |
| 3 | 50 | Agentic AI (centerpiece) |
| 4 | 37 | Frontend |
| 5 | 24 | Production readiness |
| **Total** | **157** | |

## Suggested Jira Labels

`backend` · `frontend` · `ai-agent` · `security` · `devops` · `testing` · `documentation`

## Suggested Board Columns

`Backlog` → `To Do` → `In Progress` → `Code Review` → `Testing` → `Done`
