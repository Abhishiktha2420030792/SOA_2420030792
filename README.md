# CareBridge AI — Agentic Healthcare Support and Referral Platform

A complete, hackathon/B.Tech-evaluation-ready implementation: SOA microservices backend,
an agentic AI layer (Gemini + LangGraph-style orchestration), a React + Tailwind frontend,
tests, Docker deployment, and CI/CD.

## Project Structure

```
CareBridge-AI/
 ├── backend/                    # 9 Maven modules - see backend/README.md
 │    ├── common-lib/
 │    ├── eureka-server/
 │    ├── api-gateway/
 │    ├── auth-service/
 │    ├── patient-service/
 │    ├── provider-service/
 │    ├── appointment-service/
 │    ├── notification-service/
 │    └── ai-agent-service/       # Gemini + LangGraph-style agent - the centerpiece
 ├── frontend/                    # React + Tailwind SPA
 ├── postman/                     # End-to-end API test collection
 ├── docs/
 │    ├── CareBridge-AI-Architecture.md    # Full architecture design doc
 │    ├── JIRA-PROJECT-PLAN.md             # Epics, sprints, user stories
 │    └── DEMO-AND-PITCH-GUIDE.md          # Pitch script, demo flow, Q&A prep
 ├── docker-compose.yml            # Brings up MongoDB + all 8 services + frontend
 ├── .env.example
 └── .github/workflows/ci-cd.yml   # Build, test, and Docker-build pipeline
```

## Quickest Path to a Running Demo

```bash
cp .env.example .env
# edit .env: paste a real GEMINI_API_KEY (https://aistudio.google.com/apikey)

cd backend && mvn clean package -DskipTests && cd ..
docker compose up --build
```

Then open:
- Frontend: http://localhost:3000
- Eureka dashboard: http://localhost:8761
- API Gateway (all traffic): http://localhost:8080

Full step-by-step Postman flow, port reference, and manual (non-Docker) run instructions
are in [`backend/README.md`](backend/README.md).

## Running Tests

**Backend** (from `backend/`):
```bash
mvn test
```
Covers: `AuthService` unit tests, `AuthController` integration test (MockMvc),
`ProviderService` unit tests, `RiskAssessmentNode` safety-critical unit tests, and
`AgentGraphExecutor` routing tests (verifies the graph takes the correct path for
emergency / doctor-search / answer-only / resumed-confirmation scenarios).

**Frontend** (from `frontend/`):
```bash
npm test        # unit tests (Vitest)
npm run build   # production build check
```

**API / integration** — import [`postman/CareBridge-AI.postman_collection.json`](postman/CareBridge-AI.postman_collection.json)
into Postman and run the whole collection (or use Postman Agent Mode to execute and
validate it autonomously). Covers registration, RBAC negative tests, the full agentic
booking flow, the emergency guardrail, and verification of the service-to-service side
effects (appointment created, notification dispatched).

## Where Each Requirement Lives

| Requirement | Where |
|---|---|
| SOA / Microservices | `backend/` — 8 independently deployable services + Eureka + Gateway |
| Agentic AI (Gemini + LangGraph pattern) | `backend/ai-agent-service/src/main/java/com/carebridge/agent/graph/` |
| JWT + RBAC | `backend/common-lib` (shared) + each service's `config/SecurityConfig.java` |
| Database per service | Each service's own MongoDB database, see `backend/README.md` §6 |
| React + Tailwind frontend, 9 pages | `frontend/src/pages/` |
| Testing | `backend/**/src/test/`, `frontend/src/**/__tests__/`, `postman/` |
| Docker | `backend/*/Dockerfile`, `frontend/Dockerfile`, root `docker-compose.yml` |
| CI/CD | `.github/workflows/ci-cd.yml` |
| Jira plan | `docs/JIRA-PROJECT-PLAN.md` |
| Architecture doc | `docs/CareBridge-AI-Architecture.md` |
| Demo & pitch prep | `docs/DEMO-AND-PITCH-GUIDE.md` |

## Honest Caveats (read before a live demo)

- Maven Central and the Gemini API are unreachable from the sandbox this was built in, so
  the backend has **not** been compiled or run end-to-end there — only carefully written
  and reviewed. Run `mvn clean install` locally first. The frontend **has** been installed,
  unit-tested, and production-built successfully in-sandbox (npm registry was reachable).
- See `backend/README.md` §9 for the full list of deliberate simplifications (LangGraph
  reimplemented in Java, mocked notification delivery, etc.) — worth reading once so
  you can address them confidently if asked.
