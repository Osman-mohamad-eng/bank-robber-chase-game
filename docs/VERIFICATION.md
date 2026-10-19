# Verification record

This record distinguishes executed checks from implemented tests and proposed validation. Status below is the evidence available on **2026-10-18**. No coverage percentage, performance benchmark, successful container deployment, or end-to-end gameplay claim is implied.

## Checks executed

| Check | Result and scope |
| --- | --- |
| Maven wrapper | Passed: official Wrapper 3.3.4 launches pinned Maven 3.9.11 with Java 17 |
| Core Java/gameplay suite | Passed: 222 tests started, 222 successful, zero failed/skipped, across 23 containers using a local JUnit 5.11.0 / Platform 1.11.0 launcher |
| Guard/exit arbitration and telemetry | Included above: real movement-timer regression, collection/reset counters, immutable result snapshots, and one result per attempt |
| Dashboard strict TypeScript checks | Passed using the embedded Node runtime and TypeScript compiler |
| Dashboard Node tests | Passed: 12 tests using the embedded Node runtime |

The core Java run compiled gameplay and non-HTTP telemetry code with Java 17 and ran the existing gameplay/UI tests plus `SessionTelemetryTest`. Its local log is `.tools/game-tests.log` in the development workspace; `.tools/` is ignored and is not a distributable verification artifact.

The compiler emitted a Windows sandbox path-access error while closing a JAR file, despite producing classes and returning zero. The test process also returned zero with the counts above. This was a cached-library launcher run, not a clean full Maven build. Dashboard checks used the installed editor's Node/TypeScript runtimes rather than a normal npm/Vite build.

## Checks still pending

| Check | Current status |
| --- | --- |
| Full game Maven test/package | Not yet verified, including dependency resolution and executable JAR packaging |
| HTTP client and pending-result tests | Implemented; not yet executed |
| Backend unit/controller tests | Implemented; not yet executed |
| PostgreSQL migrations, constraints and API integration tests | Implemented with Testcontainers; not yet executed |
| Dashboard Vite production build | Not yet verified |
| Docker image builds and Compose configuration/startup | Not yet verified |
| API smoke assertions against running services | Script written; not yet executed |
| CI verification workflow | Defined; no successful execution recorded |
| Desktop GUI to PostgreSQL to dashboard workflow | Not yet exercised end to end |

The development environment initially had no Docker CLI/engine or standalone Node/npm. Maven was provisioned locally. Some dependency/download operations could not execute because automatic permission approval review timed out or its model was at capacity; those are not recorded as test failures. Update this record only after actual command results are available.

## Reproduce the normal checks

Install JDK 17, Node.js 24, and Docker with a working Compose plugin. Set `JAVA_HOME` to the JDK. Run from the repository root:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd -f BankHeist/pom.xml package
.\mvnw.cmd -f backend/pom.xml -Pintegration verify
npm --prefix web install
npm --prefix web run build
```

On Linux/macOS, use `bash ./mvnw` instead of `.\mvnw.cmd`. Maven needs network access on the first run. Normal Maven tests do not require a running database; the explicit `integration` profile requires Docker and starts a PostgreSQL 18 Testcontainer. Inspect `BankHeist/target/surefire-reports`, `backend/target/surefire-reports`, and `backend/target/failsafe-reports` for results. The game's JaCoCo report is generated under `BankHeist/target/site/jacoco/` when its Maven tests run; no report or percentage has been verified here.

## Verify the local deployment

Copy `.env.example` to `.env`, choose `DB_PASSWORD`, and run:

```powershell
.\scripts\verify-compose.ps1
```

The script validates Compose configuration, builds and waits for healthy services, runs the Node API smoke script, and checks the dashboard returns HTTP 200. It leaves services running. Equivalent manual commands are:

```text
docker compose config --quiet
docker compose up --build --wait --wait-timeout 180
node scripts/smoke-api.mjs
```

Then open `http://localhost:3000` and check `http://localhost:8080/actuator/health/readiness`. `scripts/smoke-api.mjs` uses `API_URL` when set, otherwise `http://localhost:8080`. With custom ports, set `API_URL` and pass the dashboard URL to `verify-compose.ps1` using `-DashboardUrl`.

The smoke script creates a uniquely named persistent player plus a win and a loss. It checks registration, duplicate/conflicting submissions, unchanged rewards after retry, statistics, history pagination, a stored result, and ordering/uniqueness of all three leaderboards. It deletes no existing or newly created data. These health/smoke checks do not exercise the Swing application.

Set `SMOKE_SAVE_STATE` to a writable JSON path during a successful smoke run to save its test identity. After stopping and restarting services, run with `SMOKE_VERIFY_STATE` pointing to that file to assert that its accepted sessions, gross cash, and reward points survived. The verification mode reads the saved player rather than creating another one.

The [GitHub Actions workflow](../.github/workflows/verify.yml) defines Java 17 Maven/Testcontainers checks, a Node 24 dashboard build, and Compose/API/dashboard checks followed by this restart-persistence assertion. The local PowerShell helper does not itself perform that restart. These are configured checks, pending actual execution.

## Complete the desktop acceptance path

1. Start the verified Compose stack, package the desktop game, and launch it with a new valid username.
2. Finish a win and a loss. Copy the UUID from the game services window and inspect that player's dashboard statistics, recent attempts, and quests.
3. Confirm gross cash, diamonds, penalties, final score, and outcome match the actual attempts. Check pauses and the opening transition are excluded from active time.
4. Stop the API temporarily, complete another attempt, and confirm a pending JSON file appears. Start the API, wait for readiness and the retry interval, and verify one stored session and no duplicate reward.
5. Stop/restart Compose without removing its named volume. Confirm existing history, statistics, quest progress, and rewards remain.
6. Check score, money, and time pages with a level filter and pagination. Confirm one result per player and wins only on the time board.
7. Record command outcomes and any failures here before describing the local architecture as fully verified.

Kubernetes manifests and deployment work remain deferred until local Docker/PostgreSQL/API checks succeed.
