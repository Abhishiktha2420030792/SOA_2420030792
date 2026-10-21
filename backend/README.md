# CareBridge AI — Backend (Spring Boot 3 + Spring Cloud Microservices)

Agentic Healthcare Support and Referral Platform. This is the backend implementation of the
architecture approved earlier: 8 Spring Boot 3 services (2 infrastructure + 6 business
services), Netflix Eureka service discovery, Spring Cloud Gateway, JWT auth with
role-based authorization, MongoDB (database-per-service), and a Gemini-powered,
LangGraph-style agentic AI orchestrator.

## 1. Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| MongoDB | 7.x | Local install, or use the bundled `docker-compose.yml` |
| Docker & Docker Compose | latest | Optional but recommended |
| Gemini API key | — | Get one free at https://aistudio.google.com/apikey |
| Postman | latest | For testing (Postman Agent Mode collections recommended) |

> ⚠️ This code was written and reviewed carefully, but **it has not been compiled** in
> the environment it was generated in (no access to Maven Central there). Run
> `mvn clean install` locally first and fix any small issues that surface (dependency
> version mismatches are the most likely, given library ecosystems move fast) before a
> live demo.

## 2. Project Structure

```
carebridge-ai/
 ├── pom.xml                    # parent (dependency management, module list)
 ├── common-lib/                # shared JwtUtil, Role enum, ApiResponse, exceptions
 ├── eureka-server/              # port 8761
 ├── api-gateway/                 # port 8080 - single entry point
 ├── auth-service/                # port 8081
 ├── patient-service/             # port 8082
 ├── provider-service/            # port 8083 (doctors)
 ├── appointment-service/         # port 8084 (appointments + referrals)
 ├── notification-service/        # port 8085
 ├── ai-agent-service/            # port 8086 - Gemini + LangGraph-style orchestrator
 ├── docker-compose.yml
 └── .env.example
```

## 3. Build

From the repo root (builds `common-lib` first since every service depends on it,
then all services in dependency order — Maven's reactor handles this automatically
via the parent `pom.xml`'s `<modules>` list):

```bash
mvn clean install -DskipTests
```

This produces a runnable `target/<service>-1.0.0.jar` (Spring Boot fat jar) inside
every module.

## 4. Run — Option A: Docker Compose (recommended for a demo)

> `docker-compose.yml` now lives at the **CareBridge-AI project root** (one level up from
> this `backend/` folder) since it also builds the `frontend/` image. Run these commands
> from the project root, not from inside `backend/`.

```bash
cd .. # if you're inside backend/, go to the CareBridge-AI root
cp .env.example .env
# edit .env and paste your real GEMINI_API_KEY

cd backend && mvn clean package -DskipTests && cd ..   # produces the jars docker-compose.yml expects
docker compose up --build
```

This starts, in order: MongoDB → Eureka Server → API Gateway → all 6 business services.
Give it 30–60 seconds for every service to register with Eureka.

- Eureka dashboard: http://localhost:8761 (watch services appear here — great live demo visual)
- Gateway (all API traffic goes through this): http://localhost:8080

## 5. Run — Option B: Manually (useful while developing one service)

```bash
# terminal 1
cd eureka-server && mvn spring-boot:run

# terminal 2 (wait for eureka-server to be up first)
cd api-gateway && mvn spring-boot:run

# terminals 3-8, any order, once eureka-server is up:
cd auth-service && mvn spring-boot:run
cd patient-service && mvn spring-boot:run
cd provider-service && mvn spring-boot:run
cd appointment-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run

# needs GEMINI_API_KEY set in your shell:
export GEMINI_API_KEY=your-key-here
cd ai-agent-service && mvn spring-boot:run
```

Make sure a local MongoDB is running on `localhost:27017` (or override `MONGODB_URI`
per service via environment variable / `application-local.yml`).

## 6. Port Reference

| Service | Port | Mongo Database |
|---|---|---|
| Eureka Server | 8761 | — |
| API Gateway | 8080 | — |
| Auth Service | 8081 | carebridge_auth_db |
| Patient Service | 8082 | carebridge_patient_db |
| Provider Service | 8083 | carebridge_provider_db |
| Appointment Service | 8084 | carebridge_appointment_db |
| Notification Service | 8085 | carebridge_notification_db |
| AI Agent Service | 8086 | carebridge_agent_db |

**All client/Postman traffic should go through the Gateway on port 8080** — hitting a
service's own port directly skips the Gateway's JWT edge-validation (each service still
independently re-validates the JWT, so it's not a security hole, just not the intended
demo path).

## 7. End-to-End Test Flow (Postman or curl)

All requests below go to `http://localhost:8080` (the Gateway).

### 7.1 Register a patient

```
POST /api/auth/register
Content-Type: application/json

{
  "name": "Asha Verma",
  "email": "asha@example.com",
  "password": "password123",
  "phone": "9876543210",
  "role": "PATIENT"
}
```
Response includes `accessToken` — save it as `{{patientToken}}`.

### 7.2 Register a doctor

```
POST /api/auth/register
{
  "name": "Dr. Kabir Rao",
  "email": "kabir.rao@example.com",
  "password": "password123",
  "phone": "9876500000",
  "role": "DOCTOR"
}
```
Save the returned token as `{{doctorToken}}`.

### 7.3 Create the patient profile

```
POST /api/patients
Authorization: Bearer {{patientToken}}
{
  "name": "Asha Verma",
  "email": "asha@example.com",
  "dob": "1994-05-20",
  "gender": "FEMALE",
  "bloodGroup": "O+",
  "allergies": ["penicillin"],
  "chronicConditions": [],
  "currentMedications": [],
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001"
}
```

### 7.4 Create the doctor profile + a slot

```
POST /api/doctors
Authorization: Bearer {{doctorToken}}
{
  "name": "Dr. Kabir Rao",
  "email": "kabir.rao@example.com",
  "specialty": "Cardiology",
  "qualifications": ["MBBS", "MD Cardiology"],
  "hospital": "Mumbai City Hospital",
  "city": "Mumbai",
  "consultationFee": 800,
  "yearsOfExperience": 12
}
```

```
POST /api/doctors/me/slots
Authorization: Bearer {{doctorToken}}
{ "date": "2026-08-10", "startTime": "10:00", "endTime": "10:30" }
```

### 7.5 Talk to the AI agent (the main event)

```
POST /api/agent/chat
Authorization: Bearer {{patientToken}}
{ "message": "I've had chest tightness and shortness of breath since yesterday, can you find me a doctor?" }
```

The response returns a `sessionId` — reuse it on the next call. If not flagged as an
emergency, the agent will have called `provider-service`'s search endpoint live and
returned 1–3 cardiologist options (`proposedOptions`) with `awaitingConfirmation: true`.

```
POST /api/agent/chat
Authorization: Bearer {{patientToken}}
{ "sessionId": "<same sessionId>", "message": "the first one please, go ahead and book it" }
```

The agent resolves your reply against the offered options, fetches a real open slot from
`provider-service`, and books it through `appointment-service` — which itself calls
`provider-service` (mark slot booked) and `notification-service` (send confirmation).
Check the `notification-service` container logs — you'll see the mocked email log line
appear live, which is a strong demo moment.

### 7.6 Inspect the audit trail (explainability)

```
GET /api/agent/sessions/<sessionId>/audit-log
Authorization: Bearer {{patientToken}}
```

Shows every node/tool call the agent made for that conversation, plus Gemini's stated
reasoning at the decision step — this is your answer to "how do you know the AI didn't
do something wrong?" during evaluation.

### 7.7 Try the emergency guardrail

```
POST /api/agent/chat
Authorization: Bearer {{patientToken}}
{ "message": "I can't breathe and my chest really hurts" }
```

This should short-circuit straight to the emergency-escalation response, with no doctor
search or booking attempted — a good thing to demo deliberately to show the safety design.

## 8. Service-to-Service Communication (where to point evaluators)

- `appointment-service` → `provider-service`: `ProviderClient` (Feign) marks a slot booked
  after a confirmed appointment. See `appointment-service/.../client/ProviderClient.java`.
- `appointment-service` → `notification-service`: `NotificationClient` (Feign) fires a
  best-effort confirmation notification.
- `ai-agent-service` → `patient-service`, `provider-service`, `appointment-service`: the
  agent's own Feign clients — the literal "tool calls" in the LangGraph-style flow. See
  `ai-agent-service/.../client/`.
- All Feign calls propagate the original caller's JWT via a `RequestInterceptor`
  (`PropagateAuthConfig` / `FeignConfig`), so downstream services see the same identity
  and enforce the same role checks whether the call came from a human or the agent.
- All service names (`provider-service`, `patient-service`, etc.) resolve dynamically
  through Eureka — nothing is hardcoded to `localhost:PORT`.

## 9. Known Simplifications (be upfront about these if asked)

- **LangGraph itself is a Python library.** Since the stack is Spring Boot, the agent's
  state graph is hand-implemented in Java (`ai-agent-service/.../graph/`) following the
  exact same node/edge/state-graph pattern LangGraph uses. `AgentGraphExecutor` is the
  edge set; each class in `graph/nodes` is a node; `AgentState` is the shared state
  object. This is a legitimate, explainable design choice — call it out proactively in
  your pitch rather than letting a judge "catch" it; it demonstrates you understood the
  pattern well enough to reimplement it, not just import a library.
- **Notification delivery is mocked** (logged, not actually emailed/texted) — swap the
  body of `NotificationService.dispatch()` for a real `JavaMailSender`/Twilio call
  when you're ready; nothing else in the system needs to change.
- **A single MongoDB container hosts all databases** in `docker-compose.yml` for
  simplicity — they're still fully separate logical databases (true database-per-service
  isolation), just sharing one `mongod` process instead of one container each. Fine for
  a demo; trivial to split into separate containers later if you want to show that too.
- **Config Server was omitted** for build simplicity — secrets are passed via environment
  variables instead (see `.env.example`), which is a reasonable and common alternative.
  Worth mentioning as a "future scope" item (Spring Cloud Config / Vault) in your pitch.

## 10. Next Steps

1. `mvn clean install` locally, fix any dependency-version issues that surface.
2. Get a Gemini API key and confirm the model name in `application.yml`
   (`gemini.model`) still matches what's current in Google's docs.
3. Build the React + Tailwind frontend against the Gateway's REST contract
   (all endpoints above are what the frontend will call).
4. Wire up real notification delivery once the demo flow is solid.
5. Add GitHub Actions CI (build + test each module on PR) per the original roadmap.
