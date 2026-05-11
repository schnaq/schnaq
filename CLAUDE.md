# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

schnaq is a full-stack Clojure/ClojureScript discussion platform. The backend is a Clojure REST API using Datomic as its database, and the frontend is a ClojureScript SPA using re-frame/reagent compiled via shadow-cljs.

## Common Commands

### Backend

```bash
# Start backend server (REPL-driven development preferred)
clojure -M:run-server

# Run backend tests (uses Kaocha with in-memory Datomic)
clojure -M:test

# Run a single test namespace
clojure -M:test --focus schnaq.database.main-test

# Build uberjar
clojure -T:build uber

# Lint with clj-kondo
clj-kondo --lint src/

# Check outdated dependencies
clojure -M:outdated
```

### Frontend

```bash
# Install JS dependencies
yarn install

# Start shadow-cljs dev server (watches :app build, serves at localhost:8700)
clojure -M:frontend

# Compile and minify CSS (SCSS -> CSS via sass)
yarn css:minify

# Watch CSS for changes
yarn css:watch

# Run ClojureScript tests (karma-based headless)
clojure -M:test-cljs
yarn karma start --single-run

# Full production build (CSS + shadow-cljs release)
yarn build
```

### REPL Development

The nREPL starts on port 8777 (configured in shadow-cljs.edn). Backend server can be started from the REPL:

```clojure
(mount/start)   ; start all components (db, server, etc.)
(mount/stop)    ; stop all components
```

## Architecture

### Source Layout

- `src/main/` — all production source code (both CLJ and CLJS)
- `src/test/` — all test code
- `dev/` — development configuration (`config.edn` with local env vars)
- `resources/public/` — static assets served by nginx in production (CSS, compiled JS)

### Backend (Clojure)

- **Entry point**: `schnaq.api` — http-kit server with reitit router, Swagger UI at `/`
- **State management**: mount (`defstate`) for database connection, server, spec instrumentation
- **Database**: Datomic (peer library). Schema in `schnaq.database.models`, queries/transactions through `schnaq.database.main`
- **API routes**: Each domain has its own route namespace (`schnaq.api.discussion`, `schnaq.api.user`, `schnaq.api.poll`, etc.) exporting a `*-routes` var composed into the main router
- **Authentication**: Keycloak JWT tokens, validated via buddy-auth. Middleware chain in `schnaq.api.middlewares` and `schnaq.auth.middlewares`
- **Middleware registry**: Custom reitit middleware registry for authorization checks (`:user/authenticated?`, `:user/admin?`, `:discussion/valid-share-hash?`, etc.)
- **WebSockets**: sente-based, handler in `schnaq.websockets`
- **Configuration**: `yogthos/config` reads from `dev/config.edn` (dev) or environment variables (production). Shared config in `schnaq.config.shared` (cljc)
- **Specs/Validation**: clojure.spec + guardrails (`>defn`) for function contracts
- **Notification service**: Separate entry point at `schnaq.notification-service.core`

### Frontend (ClojureScript)

- **Entry point**: `schnaq.interface.core/init` — mounts reagent root component
- **State**: re-frame (events, subscriptions, effects). HTTP calls via `day8.re-frame/http-fx`
- **Routing**: reitit frontend router in `schnaq.interface.routes`
- **Components**: `schnaq.interface.components.*` (buttons, navbar, rich text via Lexical editor, icons)
- **Views**: `schnaq.interface.views.*` (discussion, feed, hub, user settings, admin, etc.)
- **Styling**: Bootstrap 5 + custom SCSS partials in `resources/public/css/`, compiled via `sass` CLI
- **i18n**: `taoensso/tempura` with translations in `schnaq.interface.translations.{english,german}`
- **JS interop**: `binaryage/oops` for JavaScript object access

### Shared Code (cljc)

Files with `.cljc` extension are shared between backend and frontend: `schnaq.config.shared`, `schnaq.database.specs`, `schnaq.export`, `schnaq.links`, `schnaq.user`, `schnaq.test-data`.

### Testing

- **Backend tests**: Kaocha runner, uses in-memory Datomic (`datomic:mem://test-db`). Test fixtures in `schnaq.test.toolbelt` handle DB lifecycle and JWT token generation
- **Frontend tests**: shadow-cljs compiles to karma target, run via `karma start --single-run`
- **Test helpers**: `schnaq.test.toolbelt` provides `init-test-delete-db-fixture`, mock auth tokens, and a `test-app` (ring handler without websockets)

### Deployment

- Backend: Dockerfile builds uberjar, runs on JVM 17 (port 3000)
- Frontend: Dockerfile builds via shadow-cljs release, served by nginx (port 80)
- CI: GitHub Actions for linting (`clj-kondo`), backend tests, and ClojureScript tests

## Key Conventions

- Route names follow `:api.domain/verb` pattern (e.g., `:api.schnaq/get`)
- Database entities use namespaced keywords (`:discussion/share-hash`, `:user.registered/email`)
- Share hashes (UUIDs) identify discussions publicly; `share-hash` is the primary lookup key for discussions
- The `>defn` macro from guardrails is used extensively for runtime spec checking in development (disabled in production builds)
- Frontend re-frame events/subscriptions are keyword-namespaced by domain

## Linear Project

All planning, dependency-refresh, modernization and tech-debt work for this repository is tracked in the Linear project **"schnaq app"**.

- URL: https://linear.app/schnaq/project/schnaq-app-44e326ace3f8/overview
- Project ID: `15fbe12c-2114-432f-b515-4d82515b1c07`
- Team: `schnaq` (key `SNQ`, ID `b65a624c-9070-4383-af64-77bd30e48c21`)
- Status workflow: Backlog → Todo → In Progress → In Review → Done
- Default assignee/lead: Christian (`christian@schnaq.com`)

When creating new issues for this repository, attach them to this project unless explicitly told otherwise.

### Label conventions

Use these labels on Linear issues that touch this repo. Combine freely:

- `dependencies` — any dependency bump (npm, deps.edn)
- `tooling` — CI, build pipeline, hooks, linters, Dockerfiles
- `security` — auth, CodeQL, secrets, supply-chain
- `tech-debt` — code cleanup, redundancies, refactors
- `frontend` — CLJS/JS/CSS scope
- `backend` — Clojure/Datomic scope
- `infra` (existing) — Docker, nginx, deploy
- `spike` (existing) — research/eval issues, no implementation expected
- `docs` (existing) — documentation updates
- Priority: `prio::high` / `prio::medium` / `prio::low`

## Modernization & Dependency Refresh

The repo is undergoing a coordinated modernization pass (Linear project above). Phased plan:

1. **Foundation & Tooling** — Renovate, CodeQL, dependency-review, Dockerfile Node 22, shadow-cljs sync, ESLint/Prettier.
2. **Safe Bumps** — Minor/patch sweeps for npm and `deps.edn`.
3. **Major Bumps** — Each major version jump (Lexical, Keycloak-js, framer-motion, FontAwesome, js-joda, date-fns, uuid, react-markdown, vis-network, …) as an isolated issue with its own QA plan.
4. **Tech-Debt & Research** — Drop Compojure, resolve Ring 1.10 pin, evaluate Karma → modern test runner, Datomic on-prem evaluation.

### Branching & PR conventions for this work

- Feature branches: `claude/<short-scope>` (e.g., `claude/update-dependencies-9RX65`, `claude/bump-lexical`).
- Base branch: `develop`.
- One Linear issue ↔ one branch ↔ one PR where possible. Link the Linear issue in the PR description (Linear auto-detects `SNQ-<n>` mentions).
- Each dependency-bump PR must run a green `yarn build` and `clojure -M:test` locally before opening.
- For UI-relevant frontend bumps, manually smoke-test the affected views via `clojure -M:frontend` on `http://localhost:8700`.
