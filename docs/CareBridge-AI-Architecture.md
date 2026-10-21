# CareBridge AI – Agentic Healthcare Support and Referral Platform

**System Architecture & Design Document**

---

## 1. Complete System Architecture

### 1.1 High-Level Overview

CareBridge AI is a **Service-Oriented Microservices Architecture (SOA)** where an **agentic AI orchestrator** sits at the center of the platform — not as a chat widget bolted on top, but as a first-class service that perceives user intent, plans multi-step actions, calls other services autonomously, and completes real workflows (booking appointments, generating referrals, escalating urgent cases) with minimal human input.

```
                                   ┌─────────────────────────┐
                                   │   React + Tailwind SPA  │
                                   │  (Patient / Doctor /    │
                                   │   Admin dashboards)     │
                                   └────────────┬─────────────┘
                                                │ HTTPS (JWT)
                                   ┌────────────▼─────────────┐
                                   │   Spring Cloud Gateway    │
                                   │  (routing, JWT filter,    │
                                   │   rate limiting, CORS)    │
                                   └────────────┬─────────────┘
                                                │
                        ┌───────────────────────┼───────────────────────────┐
                        │                       │                           │
              ┌─────────▼────────┐   ┌─────────▼─────────┐      ┌──────────▼─────────┐
              │  Eureka Discovery │   │   Config Server    │      │   Auth Service      │
              │      Server       │   │ (centralized cfg)  │      │ (JWT + RBAC)        │
              └───────────────────┘   └────────────────────┘      └─────────────────────┘
                        ▲                       ▲                           ▲
      ┌─────────────────┼───────────────────────┼───────────────────────────┼─────────────────┐
      │                 │                       │                           │                 │
┌─────▼──────┐  ┌───────▼───────┐  ┌────────────▼───────────┐  ┌────────────▼─────┐  ┌────────▼────────┐
│  Patient    │  │  Doctor /      │  │   AI Agent Orchestrator │  │  Appointment &    │  │  Notification    │
│  Service    │  │  Provider      │  │   Service (LangGraph +  │  │  Referral Service │  │  Service         │
│  (MongoDB)  │  │  Service       │  │   Gemini API)           │  │  (MongoDB)        │  │  (Email/SMS/Push)│
│             │  │  (MongoDB)     │  │   [Stateless, calls     │  │                   │  │  (MongoDB)       │
└─────────────┘  └────────────────┘  │   other services as     │  └───────────────────┘  └──────────────────┘
                                      │   "tools"]              │
                                      └──────────────────────────┘
```

All inter-service registration happens through **Eureka**. All external traffic enters through **Spring Cloud Gateway**, which validates JWTs before forwarding requests. Each microservice owns its **own MongoDB database** (database-per-service pattern) — no service reaches into another's data store directly; everything goes through REST APIs or the Agent Orchestrator.

### 1.2 Why "Agentic" and not a Chatbot

| Traditional Chatbot | CareBridge AI Agent |
|---|---|
| Answers questions from a static knowledge base | Understands goals ("I have chest pain and need a cardiologist near me tomorrow") |
| Single-turn Q&A | Multi-step planning across several services |
| No memory of workflow state | LangGraph maintains a **stateful graph** of the task (intent → data gathering → decision → action → confirmation) |
| Cannot take action | Calls Patient Service, Doctor Service, Appointment Service as **tools** and actually books/refers |
| No autonomy | Decides *which* service to call, *in what order*, and *whether to ask the user for missing info* before proceeding |

### 1.3 Core Architectural Principles

- **SOA Microservices** – each service is independently deployable, owns its data, communicates via REST (synchronous) with room to add async messaging (Kafka/RabbitMQ) later as a "future scope" talking point.
- **Database per Service** – MongoDB instance/collection set per service, enforced data isolation.
- **Centralized Auth, Decentralized Authorization** – Auth Service issues JWTs; every downstream service validates the JWT signature locally (stateless) and enforces role checks (`PATIENT`, `DOCTOR`, `ADMIN`) via Spring Security.
- **Agent-as-a-Service** – the AI Orchestrator is a peer microservice, registered in Eureka, calling other services over REST just like the Gateway would — this is the "wow factor" for evaluators because it shows the AI isn't a UI trick, it's part of the SOA fabric.
- **Config externalization** – Spring Cloud Config Server (or `.env` + Spring profiles for MVP) keeps secrets like the Gemini API key out of code.

---

## 2 & 3. Microservices Design and Responsibilities

You asked for a minimum of 5 — here are **7**, giving you enough surface area for a strong hackathon story while staying buildable in a semester timeline. If time is short, the four marked **(Core MVP)** are non-negotiable; the rest are easy to demo as "stretch" services.

### 1. **Eureka Discovery Server** (Infra)
- Service registry. Every microservice registers itself on startup.
- No business logic, no database.

### 2. **Spring Cloud Gateway** (Infra)
- Single entry point for the frontend.
- Routes `/api/patients/**`, `/api/doctors/**`, `/api/appointments/**`, `/api/agent/**`, `/api/auth/**` to the right service via Eureka service discovery.
- Global JWT validation filter — rejects unauthenticated requests before they even reach a service.
- CORS handling, request logging, basic rate limiting.

### 3. **Auth Service (Core MVP)**
- User registration & login for Patients, Doctors, Admins.
- Password hashing (BCrypt), JWT issuance (access + refresh tokens).
- Role-Based Authorization: embeds `role` claim in JWT.
- **DB (MongoDB):** `users` collection — `{ id, name, email, passwordHash, role, createdAt }`.

### 4. **Patient Service (Core MVP)**
- Patient profile CRUD: demographics, medical history, allergies, current medications.
- Symptom log entries (used by the AI agent to build context).
- **DB (MongoDB):** `patients`, `medicalHistory`, `symptomLogs`.

### 5. **Doctor / Provider Service (Core MVP)**
- Doctor profile CRUD: specialty, hospital/clinic, years of experience, consultation fee.
- Availability slots (calendar).
- Search doctors by specialty + location (used heavily by the agent for referrals).
- **DB (MongoDB):** `doctors`, `availabilitySlots`, `specialties`.

### 6. **Appointment & Referral Service (Core MVP)**
- Book / reschedule / cancel appointments.
- Generate referral records when the AI agent decides a specialist is needed (`referredBy: "AI_AGENT"` vs `"DOCTOR"` for traceability).
- Appointment status lifecycle: `REQUESTED → CONFIRMED → COMPLETED → CANCELLED`.
- **DB (MongoDB):** `appointments`, `referrals`.

### 7. **AI Agent Orchestrator Service** — *the centerpiece*
- Hosts the **LangGraph** agent graph.
- Exposes one primary endpoint: `POST /api/agent/chat` (natural language in, structured action + response out).
- Responsibilities:
  - Calls **Gemini API** for reasoning/NLU (intent classification, entity extraction, response generation).
  - Maintains conversation + workflow **state** per session (LangGraph state object).
  - Acts as an **orchestration client** to Patient Service, Doctor Service, and Appointment Service — treating each as a "tool" the LLM can invoke.
  - Implements guardrails: never diagnoses, always defers clinical judgment to a human doctor, flags emergencies for immediate escalation.
- **DB (MongoDB):** `agentSessions` (conversation + graph state history), `agentActionsLog` (audit trail of every autonomous action taken — critical for a healthcare hackathon pitch: *explainability & auditability*).

### 8. **Notification Service** (Stretch, easy win)
- Sends appointment confirmations, referral alerts, reminders.
- Mocked via console/log + optional real email (SMTP) or SMS (Twilio) integration for the demo.
- Subscribed to events from Appointment Service and Agent Orchestrator (via REST call or simple event, Kafka optional future scope).
- **DB (MongoDB):** `notifications` (delivery log).

> **Hackathon tip:** Notification Service is the cheapest way to visually prove "the agent didn't just chat — it *did* something" (a confirmation email appearing live during the demo is a strong pitch moment).

---

## 4. Database Design (MongoDB, Database-per-Service)

Each service gets its **own MongoDB database** (e.g., `carebridge_auth_db`, `carebridge_patient_db`), never shared connections.

### `carebridge_auth_db.users`
```json
{
  "_id": "ObjectId",
  "name": "string",
  "email": "string (unique, indexed)",
  "passwordHash": "string",
  "role": "PATIENT | DOCTOR | ADMIN",
  "phone": "string",
  "createdAt": "datetime",
  "isActive": "boolean"
}
```

### `carebridge_patient_db.patients`
```json
{
  "_id": "ObjectId",
  "userId": "string (ref -> auth.users._id)",
  "dob": "date",
  "gender": "string",
  "bloodGroup": "string",
  "allergies": ["string"],
  "chronicConditions": ["string"],
  "currentMedications": ["string"],
  "address": { "city": "string", "state": "string", "pincode": "string" }
}
```

### `carebridge_patient_db.symptomLogs`
```json
{
  "_id": "ObjectId",
  "patientId": "string",
  "symptoms": ["string"],
  "severity": "LOW | MEDIUM | HIGH | EMERGENCY",
  "reportedVia": "AI_AGENT | MANUAL",
  "createdAt": "datetime"
}
```

### `carebridge_doctor_db.doctors`
```json
{
  "_id": "ObjectId",
  "userId": "string (ref -> auth.users._id)",
  "specialty": "string",
  "qualifications": ["string"],
  "hospital": "string",
  "city": "string",
  "consultationFee": "number",
  "rating": "number",
  "yearsOfExperience": "number"
}
```

### `carebridge_doctor_db.availabilitySlots`
```json
{
  "_id": "ObjectId",
  "doctorId": "string",
  "date": "date",
  "startTime": "string",
  "endTime": "string",
  "isBooked": "boolean"
}
```

### `carebridge_appointment_db.appointments`
```json
{
  "_id": "ObjectId",
  "patientId": "string",
  "doctorId": "string",
  "slotId": "string",
  "status": "REQUESTED | CONFIRMED | COMPLETED | CANCELLED",
  "reasonForVisit": "string",
  "bookedBy": "PATIENT | AI_AGENT",
  "createdAt": "datetime"
}
```

### `carebridge_appointment_db.referrals`
```json
{
  "_id": "ObjectId",
  "patientId": "string",
  "fromDoctorId": "string | null",
  "toSpecialty": "string",
  "reason": "string",
  "urgency": "ROUTINE | URGENT | EMERGENCY",
  "generatedBy": "AI_AGENT | DOCTOR",
  "createdAt": "datetime"
}
```

### `carebridge_agent_db.agentSessions`
```json
{
  "_id": "ObjectId",
  "sessionId": "string",
  "patientId": "string",
  "conversationHistory": [
    { "role": "user | agent", "message": "string", "timestamp": "datetime" }
  ],
  "graphState": { "currentNode": "string", "context": "object" },
  "status": "ACTIVE | COMPLETED",
  "createdAt": "datetime"
}
```

### `carebridge_agent_db.agentActionsLog`
```json
{
  "_id": "ObjectId",
  "sessionId": "string",
  "action": "SEARCH_DOCTOR | BOOK_APPOINTMENT | CREATE_REFERRAL | ESCALATE_EMERGENCY",
  "serviceCalled": "string",
  "requestPayload": "object",
  "responsePayload": "object",
  "decisionReasoning": "string (Gemini's rationale, for explainability)",
  "timestamp": "datetime"
}
```

### `carebridge_notification_db.notifications`
```json
{
  "_id": "ObjectId",
  "recipientId": "string",
  "type": "APPOINTMENT_CONFIRMATION | REFERRAL_ALERT | REMINDER",
  "channel": "EMAIL | SMS | IN_APP",
  "status": "SENT | FAILED | PENDING",
  "createdAt": "datetime"
}
```

---

## 5. API Communication Flow

### 5.1 Standard Request Flow (non-AI)
```
React SPA → Gateway (JWT check) → Target Service → MongoDB → Response bubbles back
```

### 5.2 Agentic Workflow Flow (the key demo path)

**Example scenario:** *"I've had a fever and chest tightness since yesterday, can you find me a doctor and book something soon?"*

```
1. React SPA sends message → Gateway → AI Agent Orchestrator
   POST /api/agent/chat  { sessionId, patientId, message }

2. Agent Orchestrator loads/creates LangGraph session state
   → calls Gemini API for intent classification + entity extraction
   → Extracted: { symptoms: ["fever","chest tightness"], urgency: "possible-urgent" }

3. LangGraph "Risk Assessment" node evaluates severity
   → if EMERGENCY keywords detected → skip straight to escalation node
   → else continue normal flow

4. Agent calls Patient Service (REST, JWT passed through)
   GET /api/patients/{id}/history  → pulls allergies, chronic conditions for context

5. Gemini reasons over symptoms + history → decides specialty needed
   e.g., "Cardiologist" (chest tightness) 

6. Agent calls Doctor Service
   GET /api/doctors/search?specialty=Cardiology&city=...&availableFrom=today

7. Agent presents 2–3 doctor options back to user via chat
   (Agentic but still keeps a human-in-the-loop checkpoint for booking —
    good talking point: "autonomous but safe by design")

8. User confirms a slot in chat → Agent calls Appointment Service
   POST /api/appointments  { patientId, doctorId, slotId, bookedBy: "AI_AGENT" }

9. Appointment Service confirms → Agent calls Notification Service
   POST /api/notifications  { type: "APPOINTMENT_CONFIRMATION" }

10. Every step 4–9 is written to agentActionsLog for auditability.

11. Agent returns final structured + natural-language response to frontend.
```

### 5.3 Security Flow
```
Login → Auth Service validates credentials → issues JWT (role embedded)
       → every subsequent request carries "Authorization: Bearer <token>"
       → Gateway filter validates signature + expiry
       → downstream service's Spring Security filter chain re-checks role
         (@PreAuthorize("hasRole('PATIENT')") etc.)
```

---

## 6. AI Agent Workflow — Gemini + LangGraph

### 6.1 Why LangGraph specifically
LangGraph lets you model the agent as an explicit **state graph** rather than a single prompt-response loop — this is exactly what you want to show evaluators: a visual, explainable decision path, not a black box.

### 6.2 The Graph

```
        ┌───────────────┐
        │  START / Input │
        └───────┬────────┘
                │
        ┌───────▼────────┐
        │ Intent & Entity │   (Gemini call #1: classify intent,
        │   Extraction    │    extract symptoms/goal/entities)
        └───────┬────────┘
                │
        ┌───────▼─────────┐
        │  Risk / Urgency   │   Rule-based + Gemini hybrid check
        │  Assessment Node  │──────────────┐
        └───────┬──────────┘               │ EMERGENCY detected
                │ normal                    ▼
                │                  ┌──────────────────┐
                │                  │ Emergency Escalate │→ Notify + surface
                │                  │       Node          │  ER/helpline info
                │                  └──────────────────┘   immediately
                ▼
        ┌────────────────────┐
        │ Context Gathering    │   Tool call → Patient Service
        │ Node (get history)   │
        └───────┬───────────┘
                │
        ┌───────▼────────────┐
        │  Decision Node        │   Gemini reasons: "Does this need
        │  (Planning)            │    a doctor search? A referral?
        │                        │    Just info?" → picks next tool
        └───────┬────────────┘
                │
     ┌──────────┼───────────────┐
     ▼          ▼                ▼
┌─────────┐ ┌──────────────┐ ┌────────────────┐
│ Doctor    │ │ Referral      │ │ Informational    │
│ Search    │ │ Generation    │ │ Answer Node       │
│ Tool Node │ │ Tool Node     │ │ (Gemini answers   │
│(→ Doctor  │ │(→ Appointment │ │ directly, no      │
│ Service)  │ │ Service)      │ │ service call)     │
└─────┬─────┘ └──────┬───────┘ └────────┬─────────┘
      │              │                    │
      └──────┬────────┴────────────────────┘
              ▼
      ┌────────────────────┐
      │ Human-in-the-loop    │  Presents options to patient,
      │ Confirmation Node    │  waits for explicit "yes/confirm"
      └───────┬────────────┘
              │ confirmed
              ▼
      ┌────────────────────┐
      │  Action Execution     │  Tool call → Appointment Service
      │  Node                 │  (books/creates referral)
      └───────┬────────────┘
              ▼
      ┌────────────────────┐
      │  Notify + Log Node    │  → Notification Service
      │                        │  → agentActionsLog (audit)
      └───────┬────────────┘
              ▼
      ┌────────────────────┐
      │  Response Synthesis   │  Gemini call: turn structured
      │  Node                 │  result into natural language
      └───────┬────────────┘
              ▼
            END (returns to user, session state persisted)
```

### 6.3 Agentic Design Principles Demonstrated
- **Perception** – Gemini extracts structured intent/entities from free text.
- **Planning** – Decision Node autonomously chooses which downstream service(s) to call and in what order.
- **Tool Use** – Each microservice REST endpoint is wrapped as a callable "tool" in the LangGraph node (this is your strongest technical talking point: *"we didn't fake AI actions — the agent literally calls our Spring Boot microservices as tools"*).
- **Memory** – `agentSessions.graphState` persists conversation + workflow context across turns (multi-turn agent, not stateless Q&A).
- **Guardrails** – Emergency detection short-circuits the graph; the agent never issues a diagnosis, only flags urgency and routes to appropriate specialists; human confirmation required before any booking action executes.
- **Explainability/Audit** – `agentActionsLog` records Gemini's reasoning + every tool call, addressing the #1 concern evaluators raise about healthcare AI: *"how do we trust/trace what it did?"*

### 6.4 Gemini API Usage Points
1. Intent classification + entity extraction (structured JSON output mode).
2. Clinical-adjacent reasoning for specialty routing (with strict system prompt: *"You are a triage assistant, not a diagnostician"*).
3. Natural language response generation from structured tool results.
4. (Stretch) Summarizing a patient's symptom history for a doctor before a consultation.

---

## 7. Frontend Architecture (React + Tailwind)

### 7.1 Structure
- **Auth-aware SPA** with role-based routing (Patient / Doctor / Admin dashboards render different nav + pages based on JWT role claim).
- **Central AI Agent Chat Panel** — persistent, collapsible widget available across the patient experience, backed by `agentSessions` (so a page refresh doesn't lose context).
- State management: React Context + `useReducer` for auth/session; React Query (TanStack Query) for server state/caching per service.
- Axios instance with interceptor injecting JWT + handling 401 → refresh token flow.

### 7.2 Key Pages
| Route | Role | Purpose |
|---|---|---|
| `/login`, `/register` | Public | Auth |
| `/patient/dashboard` | Patient | Overview, upcoming appointments, AI chat entry point |
| `/patient/agent` | Patient | Full-screen agentic chat workflow view (shows live "agent is calling Doctor Service…" style status — great demo visual) |
| `/patient/appointments` | Patient | List/manage appointments |
| `/patient/history` | Patient | Medical history & symptom logs |
| `/doctor/dashboard` | Doctor | Today's appointments, patient summaries |
| `/doctor/availability` | Doctor | Manage slots |
| `/admin/dashboard` | Admin | User management, service health (Eureka status), agent audit log viewer |

### 7.3 Component Design (high level)
```
src/
 ├── components/
 │    ├── common/        (Navbar, Sidebar, ProtectedRoute, Toast)
 │    ├── chat/          (AgentChatWindow, MessageBubble, AgentActionStatus)
 │    ├── patient/       (AppointmentCard, HistoryTimeline)
 │    └── doctor/        (SlotManager, PatientSummaryCard)
 ├── pages/               (route-level pages, see table above)
 ├── context/             (AuthContext, AgentSessionContext)
 ├── hooks/               (useAuth, useAgentChat, useApi)
 ├── services/            (api clients per backend service: authApi, patientApi,
 │                          doctorApi, appointmentApi, agentApi)
 ├── utils/               (jwtDecode helpers, roleGuards)
 └── App.jsx / main.jsx
```

The **AgentActionStatus** component is a nice hackathon touch: as the LangGraph agent moves through nodes server-side, stream status updates (via polling or SSE) like *"Checking your medical history…" → "Searching cardiologists near you…" → "Preparing appointment options…"* — this visually sells "agentic" instead of "chatbot."

---

## 8. Backend Folder Structure

Recommended as a **multi-module setup within one Git repo (monorepo)** for a B.Tech/hackathon timeline — easier to manage, still visually "microservices" when presented (each module deploys as its own Docker container).

```
carebridge-ai/
 ├── eureka-server/
 │    └── src/main/java/.../EurekaServerApplication.java
 ├── api-gateway/
 │    └── src/main/java/.../gateway/
 │         ├── filter/JwtAuthFilter.java
 │         └── config/RouteConfig.java
 ├── auth-service/
 │    └── src/main/java/.../auth/
 │         ├── controller/AuthController.java
 │         ├── service/AuthService.java
 │         ├── repository/UserRepository.java
 │         ├── model/User.java
 │         ├── security/JwtUtil.java
 │         └── dto/
 ├── patient-service/
 │    └── src/main/java/.../patient/
 │         ├── controller/
 │         ├── service/
 │         ├── repository/
 │         ├── model/
 │         └── client/ (Feign client to Auth Service if needed)
 ├── doctor-service/
 │    └── (same layered structure)
 ├── appointment-service/
 │    └── (same layered structure)
 ├── notification-service/
 │    └── (same layered structure)
 ├── ai-agent-service/
 │    └── src/main/java/.../agent/
 │         ├── controller/AgentController.java
 │         ├── graph/               (LangGraph node definitions)
 │         │    ├── IntentNode.java
 │         │    ├── RiskAssessmentNode.java
 │         │    ├── DecisionNode.java
 │         │    ├── ToolNodes/ (DoctorSearchTool, ReferralTool, BookingTool)
 │         │    └── ResponseSynthesisNode.java
 │         ├── gemini/GeminiClient.java
 │         ├── client/ (Feign/RestClient wrappers for Patient/Doctor/Appointment services)
 │         ├── repository/AgentSessionRepository.java
 │         ├── model/AgentSession.java, AgentActionLog.java
 │         └── config/
 ├── common-lib/                    (shared DTOs, JWT util, exception handlers — Maven module)
 ├── docker-compose.yml             (spins up all services + MongoDB instances)
 └── .github/workflows/ci-cd.yml    (GitHub Actions: build, test, docker push per service)
```

Each service module has its **own `pom.xml`**, **own `application.yml`** (pointing to its own MongoDB URI + Eureka), and **own Dockerfile**.

---

## 9. Development Roadmap

Structured as **5 phases**, suitable for a semester-length B.Tech project with a hackathon-ready checkpoint at Phase 3.

### Phase 1 – Foundation (Week 1–2)
- Set up monorepo, Eureka Server, API Gateway, Config Server.
- Auth Service: registration/login, JWT issuance, role-based guards.
- Docker Compose for local MongoDB + all services.
- Basic React app shell with routing + auth context.
- **Milestone:** login works end-to-end through the Gateway.

### Phase 2 – Core Domain Services (Week 3–4)
- Patient Service (profile, history CRUD).
- Doctor Service (profile, availability, search).
- Appointment Service (booking, referral models).
- Frontend: dashboards, appointment booking UI (manual, non-AI path first).
- **Milestone:** a patient can manually search a doctor and book an appointment — proves the SOA plumbing works before adding AI.

### Phase 3 – AI Agent Orchestrator (Week 5–7) — **Hackathon-ready checkpoint**
- Integrate Gemini API (structured output mode for intent/entity extraction).
- Build LangGraph graph: Intent → Risk Assessment → Context Gathering → Decision → Tool Nodes → Confirmation → Action → Notify → Response.
- Wire Agent Orchestrator's tool nodes to real REST calls into Patient/Doctor/Appointment services.
- Build `AgentChatWindow` frontend with live status updates.
- Implement `agentActionsLog` for auditability.
- **Milestone:** full agentic flow — symptom description → doctor found → appointment booked → notification sent — demoable live.

### Phase 4 – Notification, Hardening, DevOps (Week 8–9)
- Notification Service (email/SMS via SMTP/Twilio, or mocked for demo).
- GitHub Actions CI/CD: build + test + Docker image push per service on PR/merge.
- Centralized exception handling, input validation, rate limiting on Gateway.
- Postman Agent Mode collections for automated API regression testing.
- Emergency escalation path polish (this is a strong ethics/safety talking point for evaluators).

### Phase 5 – Polish & Pitch Prep (Week 10)
- UI polish with Tailwind (loading states, agent "thinking" animations).
- Lucidchart AI-generated architecture diagrams for the pitch deck (matches what's in this document).
- Record a scripted demo: manual booking path (SOA proof) → AI agentic path (differentiator) → admin audit log (trust/safety proof).
- Prepare a 2-minute "why agentic, not chatbot" narrative + a slide comparing traditional vs. agentic flow (reuse the table in Section 1.2).
- Load/stress test key endpoints via Postman Agent Mode for a "production-readiness" talking point.

---

## Evaluation & Pitch Angles (worth keeping in your back pocket)

- **Technical depth:** SOA + service discovery + gateway + JWT RBAC + DB-per-service — a legitimately complete microservices implementation, not a toy.
- **AI differentiation:** LangGraph state graph + Gemini + real tool calls into your own services = genuinely agentic, defensible against "isn't this just a chatbot?" questions.
- **Healthcare responsibility:** explicit non-diagnostic guardrails, emergency escalation, human-in-the-loop confirmation before booking, full audit log — shows maturity around AI safety in a sensitive domain, which judges reward.
- **DevOps maturity:** Docker + GitHub Actions CI/CD shows you understand deployment, not just code.

---

**Next step:** once you approve this design, I can start scaffolding — I'd suggest beginning with Phase 1 (Eureka + Gateway + Auth Service + React shell) so we have a working, demoable skeleton before layering in the AI agent.
