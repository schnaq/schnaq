# AGENTS.md - AI Agent Guide for Schnaq

This document provides guidance for AI agents (like Claude Code, GitHub Copilot, etc.) working with the Schnaq codebase.

## Project Overview

**Schnaq** is an interactive education and discussion platform built with Clojure/ClojureScript. It enables educators and participants to engage in real-time Q&A sessions, discussions, polls, and collaborative learning.

- **Repository**: https://github.com/schnaq/schnaq
- **Live Application**: https://app.schnaq.com
- **Tagline**: "Education as interactive, as it's supposed to be!"

## Technology Stack

### Backend (Clojure)
- **Language**: Clojure 1.11.3
- **Web Framework**: Reitit 0.7.1 (routing + middleware)
- **HTTP Server**: http-kit 2.8.0
- **Database**: Datomic Pro (immutable, time-travel DB)
- **Real-time**: Sente 1.19.2 (WebSockets)
- **Authentication**: Keycloak + Buddy
- **Email**: Postal 2.0.5
- **Cloud Storage**: AWS S3
- **State Management**: Mount 0.1.18
- **Validation**: Spec + Guardrails 1.2.9

### Frontend (ClojureScript)
- **Language**: ClojureScript 1.11.132
- **Build Tool**: Shadow-cljs 2.28.11
- **UI Framework**: React 18 + Reagent 1.2.0
- **State Management**: Re-frame 1.4.3
- **Rich Text**: Lexical 0.4.0 (with custom plugins)
- **UI Components**: Bootstrap 5 + React Bootstrap 2.7.0
- **Styling**: SCSS/Sass + Purgecss
- **Charts**: Chart.js + React ChartJS-2

### Deployment
- **Containers**: Docker (multi-stage builds)
- **Orchestration**: Kubernetes + Helm
- **CI/CD**: GitHub Actions
- **Frontend Hosting**: Vercel
- **Backend**: Custom infrastructure

## Project Structure

### Backend Structure (`src/main/schnaq/`)

```
schnaq/
├── api.clj                    # Main HTTP server entry point, Reitit router setup
├── core.clj                   # Application initialization, stateful components
├── config.clj                 # Configuration management (env vars, secrets)
├── auth.clj                   # JWT authentication, Keycloak integration
│
├── api/                       # API route handlers (20 modules, ~2,500 lines)
│   ├── activation.clj         # Session activation endpoints
│   ├── discussion.clj         # Discussion management (31KB, largest module)
│   ├── schnaq.clj            # Main discussion CRUD operations
│   ├── qa-box.clj            # Q&A box functionality
│   ├── poll.clj              # Polling endpoints
│   ├── wordcloud.clj         # Word cloud generation
│   ├── hub.clj               # Hub/collection management
│   ├── user.clj              # User management
│   ├── feedback.clj          # Feedback collection
│   ├── themes.clj            # Theme customization
│   ├── moderation.clj        # Moderation controls
│   ├── analytics.clj         # Analytics data
│   └── middlewares.clj       # Reitit middleware (CORS, auth, errors)
│
├── database/                  # Database layer (~3,250 lines)
│   ├── main.clj              # DB connection, transactions, queries
│   ├── models.clj            # Datomic schema definitions (527 lines)
│   ├── discussion.clj        # Discussion data operations
│   ├── user.clj              # User data management
│   ├── poll.clj              # Poll data
│   ├── question_box.clj      # Q&A box data
│   ├── hub.clj               # Hub data
│   ├── feedback_form.clj     # Feedback data
│   ├── wordcloud.clj         # Word cloud data
│   ├── reaction.clj          # Reactions/votes
│   ├── themes.clj            # Theme data
│   └── access_codes.clj      # Access control
│
├── websockets/
│   └── handler.clj           # WebSocket event handling via Sente
│
├── mail/
│   └── emails.clj            # Email templates and sending
│
├── notification_service/     # Async notification processing
├── media.clj                 # Image processing
├── s3.clj                    # AWS S3 integration
├── processors.clj            # Background jobs
└── export.cljc               # Data export functionality
```

### Frontend Structure (`src/main/schnaq/interface/`)

```
interface/
├── core.cljs                 # Application entry point
├── views.cljs                # Root view router
├── events.cljs               # Re-frame event handlers
├── routes.cljs               # Client-side routing
├── auth.cljs                 # Keycloak authentication
├── config.cljs               # Client configuration
├── translations.cljs         # i18n (English, German)
│
├── components/               # Reusable UI components
│   ├── buttons.cljs
│   ├── common.cljs
│   ├── icons.cljs
│   ├── images.cljs
│   ├── videos.cljs
│   └── lexical/             # Rich text editor
│       └── plugins/         # Custom Lexical plugins
│           ├── toolbar.cljs
│           ├── images.cljs
│           ├── videos.cljs
│           ├── links.cljs
│           └── excalidraw.cljs
│
├── views/                    # Page views
│   ├── schnaq/              # Discussion creation/management
│   ├── discussion/          # Discussion rendering
│   ├── user/                # User profile/settings
│   ├── hub/                 # Hub views
│   ├── admin/               # Admin interface
│   ├── feedback/            # Feedback forms
│   ├── feed/                # Activity feed
│   ├── qa/                  # Q&A search
│   ├── pages.cljs           # Static pages
│   ├── modal.cljs           # Modal dialogs
│   └── notifications.cljs   # Toast notifications
│
└── utils/                    # Utility functions
    ├── http.cljs            # AJAX/HTTP client
    ├── localstorage.cljs    # Browser storage
    ├── markdown.cljs        # Markdown rendering
    ├── file_reader.cljs
    ├── file_download.cljs
    ├── language.cljs        # i18n utilities
    ├── time.cljs            # Time formatting
    ├── routing.cljs         # Route parsing
    └── tooltip.cljs
```

## Key Architectural Patterns

### Backend Patterns

1. **Component Lifecycle (Mount)**
   - Stateful components with `:start`/`:stop` lifecycle
   - Used for database connections, websockets, HTTP server
   - Example: `(mount/defstate db-connection :start (connect-db) :stop (disconnect-db))`

2. **Route-Based Architecture (Reitit)**
   - Data-driven routing (routes defined as Clojure data)
   - Middleware composition per route
   - Automatic Swagger/OpenAPI documentation
   - Spec-based parameter validation and coercion

3. **Multimethod Dispatch (WebSockets)**
   - Message type-based handling via `defmulti`
   - Extensible event processing
   - Example: `(defmulti handle-event :event-type)`

4. **Data Validation**
   - Clojure Spec for data validation
   - Guardrails for runtime instrumentation
   - DTOs for API contracts

5. **Database (Datomic)**
   - Immutable, time-travel database
   - Schema defined as data in `models.clj`
   - Query abstractions for consistency
   - Transaction-based updates

### Frontend Patterns

1. **Re-frame Event-Driven Architecture**
   - **Unidirectional data flow**: Events → Effects → DB updates → Subscriptions
   - **Single app state**: One centralized atom
   - **Pure functions**: All reducers are pure
   - **Effects system**: Declarative side-effects (HTTP, storage, etc.)

2. **Component Structure (Reagent/React)**
   - Functional components with hooks
   - Composable, reusable UI primitives
   - Local state with atoms where needed
   - Hiccup syntax for HTML: `[:div {:class "container"} "Hello"]`

3. **Client-Side Routing (Reitit)**
   - Data-driven route definitions
   - Nested route hierarchies
   - Synced with browser history

## Development Workflow

### Setup and Running

#### Backend Development
```bash
# Install dependencies (automatic via deps.edn)
clj -M:run-server              # Run server with REPL

# Building
clj -T:build uber              # Create standalone JAR
```

#### Frontend Development
```bash
# Install JS dependencies
yarn install

# Development (hot reload)
clj -M:frontend:dev           # Start Shadow-cljs watcher
yarn css:watch                # Watch and compile SCSS

# Production build
yarn build
```

### Testing

#### Backend Tests
```bash
clj -M:test                                    # Run all tests
clj -M:test --focus [namespace]/[function]   # Run specific test

# Code coverage
clj -M:cloverage              # Generate coverage report (HTML + LCOV)
```

#### Frontend Tests
```bash
yarn shadow-cljs compile test        # Compile test suite
node target/test/compiled/test.js    # Run tests
```

### Code Quality

```bash
# Linting
clj-kondo --lint src/main src/test  # Lint Clojure/ClojureScript

# Unused variable analysis
clj -M:unused-vars

# Dependency updates
clj -M:antq                   # Check for outdated dependencies

# CSS linting
yarn stylelint "resources/**/*.scss"
```

## Important Files and Locations

### Configuration Files
- **[deps.edn](deps.edn)** - Clojure dependencies and build config
- **[shadow-cljs.edn](shadow-cljs.edn)** - Frontend build configuration
- **[package.json](package.json)** - Node.js dependencies
- **[Dockerfile.backend](Dockerfile.backend)** / **[Dockerfile.frontend](Dockerfile.frontend)** - Container images
- **[.clj-kondo/config.edn](.clj-kondo/config.edn)** - Linting rules
- **[.stylelintrc.json](.stylelintrc.json)** - CSS linting rules

### Entry Points
- **Backend**: [src/main/schnaq/api.clj](src/main/schnaq/api.clj) - HTTP server and router
- **Frontend**: [src/main/schnaq/interface/core.cljs](src/main/schnaq/interface/core.cljs) - App initialization
- **Database Schema**: [src/main/schnaq/database/models.clj](src/main/schnaq/database/models.clj) - Datomic schema (527 lines)

### Critical Modules
- **[src/main/schnaq/api/discussion.clj](src/main/schnaq/api/discussion.clj)** - Main discussion API (31KB, largest module)
- **[src/main/schnaq/database/main.clj](src/main/schnaq/database/main.clj)** - Database operations
- **[src/main/schnaq/websockets/handler.clj](src/main/schnaq/websockets/handler.clj)** - Real-time communication
- **[src/main/schnaq/interface/events.cljs](src/main/schnaq/interface/events.cljs)** - Frontend event handlers

## Common Development Tasks

### Adding a New API Endpoint

1. **Define route in appropriate handler** (e.g., `src/main/schnaq/api/discussion.clj`):
   ```clojure
   ["/new-endpoint" {:get {:summary "Description"
                           :parameters {:query {:param-name string?}}
                           :responses {200 {:body ::response-spec}}
                           :handler (fn [request]
                                      {:status 200
                                       :body {:result "data"}})}}]
   ```

2. **Add Spec for validation** (if needed):
   ```clojure
   (s/def ::response-spec (s/keys :req-un [::result]))
   ```

3. **Update database operations** in `src/main/schnaq/database/` if needed

4. **Test the endpoint** in `src/test/schnaq/api/`

### Adding a New Frontend Feature

1. **Define event handler** in [src/main/schnaq/interface/events.cljs](src/main/schnaq/interface/events.cljs):
   ```clojure
   (re-frame/reg-event-fx
     ::new-feature
     (fn [{:keys [db]} [_ args]]
       {:db (assoc db :feature-data args)
        :http-xhrio {:method :get
                     :uri "/api/endpoint"
                     :on-success [::handle-success]}}))
   ```

2. **Define subscription** for data access:
   ```clojure
   (re-frame/reg-sub
     ::feature-data
     (fn [db _]
       (:feature-data db)))
   ```

3. **Create component** in `src/main/schnaq/interface/components/` or `views/`:
   ```clojure
   (defn new-feature-component []
     (let [data @(re-frame/subscribe [::feature-data])]
       [:div.feature-container
        [:h2 "Feature"]
        [:p (str data)]]))
   ```

4. **Add route** in [src/main/schnaq/interface/routes.cljs](src/main/schnaq/interface/routes.cljs) if needed

5. **Add tests** in `src/test/schnaq/interface/`

### Working with Database (Datomic)

1. **Define schema** in [src/main/schnaq/database/models.clj](src/main/schnaq/database/models.clj):
   ```clojure
   {:db/ident :entity/new-field
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Description of field"}
   ```

2. **Write query functions** in appropriate namespace (e.g., `src/main/schnaq/database/discussion.clj`):
   ```clojure
   (defn get-entity-by-id [db id]
     (d/pull db '[*] [:db/id id]))
   ```

3. **Perform transactions**:
   ```clojure
   (d/transact conn {:tx-data [{:entity/id id
                                :entity/new-field "value"}]})
   ```

### Adding WebSocket Event Handlers

1. **Define event multimethod** in [src/main/schnaq/websockets/handler.clj](src/main/schnaq/websockets/handler.clj):
   ```clojure
   (defmethod handle-event :new-event-type
     [event-msg]
     (let [{:keys [client-id ?data]} event-msg]
       ;; Process event
       {:status :ok}))
   ```

2. **Broadcast to clients** if needed:
   ```clojure
   (sente/send! chsk-send! client-id [:event-type {:data "value"}])
   ```

### Email Templates

- **Location**: [src/main/schnaq/mail/emails.clj](src/main/schnaq/mail/emails.clj)
- **Configuration**: Uses Postal library with SMTP settings from [config.clj](src/main/schnaq/config.clj)
- **Templates**: HTML email templates with placeholders

### File Uploads and Storage

- **S3 Integration**: [src/main/schnaq/s3.clj](src/main/schnaq/s3.clj)
- **Image Processing**: [src/main/schnaq/media.clj](src/main/schnaq/media.clj)
- **Frontend Upload**: Components in `src/main/schnaq/interface/components/`

## Important Conventions

### Code Style

#### Clojure/ClojureScript
- **Naming**: Use kebab-case for functions/vars: `(defn my-function [])`
- **Namespaces**: Use project prefix: `schnaq.api.discussion`
- **Private functions**: Use `defn-` or `^:private`
- **Specs**: Define in same namespace or separate `specs.clj`
- **Documentation**: Docstrings for public functions

#### Hiccup/HTML
- **Keywords**: Use keywords for elements: `[:div]`
- **Classes**: Use maps for attributes: `{:class "container"}`
- **IDs**: Shorthand with `#`: `[:div#my-id]`
- **Classes**: Shorthand with `.`: `[:div.my-class]`

### Database Conventions
- **Entity IDs**: Use `:db/id` or custom identity attributes
- **Namespaced attributes**: All attributes namespaced (e.g., `:discussion/title`)
- **References**: Use `:db.type/ref` for relationships
- **Cardinality**: Choose `:db.cardinality/one` or `:db.cardinality/many`

### API Conventions
- **RESTful routes**: Use HTTP verbs appropriately
- **Response format**: JSON with consistent structure
- **Error handling**: Middleware catches and formats errors
- **Authentication**: JWT tokens in Authorization header
- **CORS**: Configured in middlewares for embedding

### Re-frame Conventions
- **Event naming**: Use double-colon keywords: `::load-data`
- **Effects**: Declare all side effects (`:http-xhrio`, `:dispatch`, `:db`)
- **Subscriptions**: Keep simple, compose when needed
- **Components**: Pure functions, subscribe at top level

## Testing Guidelines

### Backend Testing
- **Unit tests**: Pure function testing
- **Integration tests**: Database operations with test DB
- **API tests**: Route handler testing with mock requests
- **Use fixtures**: Setup/teardown for test data

### Frontend Testing
- **Component tests**: Test rendering and interactions
- **Event handler tests**: Test state transformations
- **Subscription tests**: Test data derivation
- **E2E tests**: Full user workflows (if needed)

## Environment Variables

Key environment variables (defined in [config.clj](src/main/schnaq/config.clj)):

- `DATOMIC_URI` - Database connection string
- `KEYCLOAK_*` - Keycloak authentication config
- `AWS_*` - AWS S3 credentials
- `SMTP_*` - Email server configuration
- `API_URL` - Backend API URL
- `BUILD_HASH` - Version identifier

## Common Issues and Solutions

### Build Issues
- **Shadow-cljs not found**: Run `yarn install` first
- **Database connection fails**: Check Datomic transactor is running
- **SCSS not compiling**: Run `yarn css:watch` in separate terminal

### Development Issues
- **Hot reload not working**: Check Shadow-cljs watcher is running
- **WebSocket connection fails**: Verify backend server is running and CORS is configured
- **Auth errors**: Check Keycloak configuration and JWT tokens

### Testing Issues
- **Tests failing**: Ensure test database is available
- **Frontend tests fail**: Check Node.js version and dependencies
- **Coverage reports missing**: Run with `clj -M:cloverage`

## Deployment

### Docker Build
```bash
# Backend
docker build -f Dockerfile.backend -t schnaq-backend .

# Frontend
docker build -f Dockerfile.frontend -t schnaq-frontend .
```

### Kubernetes Deployment
- **Helm charts**: Located in deployment repository
- **Secrets**: Managed via Kubernetes secrets
- **Scaling**: Horizontal pod autoscaling configured

### CI/CD
- **GitHub Actions**: Automated testing and deployment
- **Branches**:
  - `develop` - Development branch (current)
  - `main` - Production branch
- **PR Workflow**: Tests must pass before merge

## Resources and Documentation

- **Clojure**: https://clojure.org/
- **ClojureScript**: https://clojurescript.org/
- **Re-frame**: https://day8.github.io/re-frame/
- **Reagent**: https://reagent-project.github.io/
- **Reitit**: https://cljdoc.org/d/metosin/reitit/
- **Datomic**: https://docs.datomic.com/
- **Shadow-cljs**: https://shadow-cljs.github.io/docs/UsersGuide.html
- **Sente**: https://github.com/taoensso/sente

## Key Takeaways for AI Agents

1. **This is a Clojure/ClojureScript project** - Use functional programming patterns
2. **Re-frame for frontend** - Follow unidirectional data flow
3. **Datomic for database** - Immutable, time-travel DB with query-based access
4. **Real-time via WebSockets** - Sente for bidirectional communication
5. **Reitit for routing** - Data-driven routes on both backend and frontend
6. **Spec for validation** - Use specs for data validation and documentation
7. **Component lifecycle** - Mount for stateful component management
8. **Testing is important** - Write tests for new features
9. **Code quality** - Run linters before committing
10. **Documentation** - Add docstrings and update this file for major changes

---

**Last Updated**: 2025-12-02
**Project Version**: See `BUILD_HASH` in environment
**Maintainer**: Schnaq Team
