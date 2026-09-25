# PR Pilot 🤖

**AI Co-Pilot for Faster, Safer Code Reviews — Powered by IBM Bob 2.0**

PR Pilot sits between your GitHub repository and your team's review process. Every time a pull request is opened or updated, it spawns five specialised subagents in parallel (logic, tests, security, style, docs), synthesises the findings, and posts a single structured review comment within minutes.

---

## Project structure

```
pr-pilot/
├── .bob/
│   ├── custom_modes.yaml          ← pr-reviewer custom Bob mode
│   └── skills/pr-pilot/
│       ├── SKILL.md               ← PR Pilot skill (activates team knowledge)
│       ├── conventions.md         ← team architecture & naming conventions
│       ├── security-checklist.md  ← OWASP-aligned security scan checklist
│       ├── test-coverage-rules.md ← required test coverage rules
│       └── severity-guide.md      ← CRITICAL / WARNING / SUGGESTION definitions
├── .github/
│   └── workflows/
│       └── pr-pilot.yml           ← GitHub Actions workflow
├── prompts/
│   ├── main-agent.md              ← master orchestration prompt
│   ├── logic-subagent.md          ← logic & correctness subagent
│   ├── test-subagent.md           ← test coverage subagent
│   ├── security-subagent.md       ← security vulnerability subagent
│   ├── style-subagent.md          ← style & conventions subagent
│   └── docs-subagent.md           ← documentation & changelog subagent
├── scripts/
│   ├── review.sh                  ← entry-point: generates diff and calls Bob Shell
│   └── post-comment.sh            ← posts review to GitHub PR via REST API
├── server/                        ← Node.js webhook server (alternative to CI)
│   ├── src/
│   │   ├── index.js
│   │   ├── logger.js
│   │   ├── routes/
│   │   │   ├── webhook.js
│   │   │   └── health.js
│   │   ├── services/
│   │   │   └── review-service.js
│   │   └── utils/
│   │       ├── env-validator.js
│   │       └── signature-verifier.js
│   ├── tests/
│   │   ├── setup.js
│   │   ├── health.test.js
│   │   ├── signature-verifier.test.js
│   │   └── webhook.test.js
│   ├── package.json
│   └── .env.example
└── sample-repo/                   ← Spring Boot demo project (library management)
    ├── pom.xml
    └── src/
        └── main/java/com/library/
            ├── LibraryApplication.java
            ├── config/SecurityConfig.java
            ├── controller/ReservationController.java
            ├── dto/ReservationRequest.java + ReservationResponse.java
            ├── exception/ (BookNotFoundException, GlobalExceptionHandler, etc.)
            ├── model/ (Book, Reservation, User, enums)
            ├── repository/ (BookRepository, ReservationRepository)
            └── service/ (ReservationService, ReservationServiceImpl)
```

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| IBM Bob Shell | latest | Runs AI-powered reviews |
| Node.js | ≥ 18 | Webhook server |
| Bash | any | CI scripts |
| Java | 17 | Sample Spring Boot app |
| Maven | ≥ 3.9 | Build sample app |
| Git | any | Diff generation |

---

## Setup

### 1. Install Bob Shell

```bash
npm install -g @ibm/bob-shell
bob --version
```

### 2. Install the Bob custom mode

Copy `.bob/custom_modes.yaml` and `.bob/skills/` into your target repository:

```bash
cp -r .bob /path/to/your/repo/
```

Or for global installation (available in all projects):

```bash
cp .bob/custom_modes.yaml ~/.bob/custom_modes.yaml
cp -r .bob/skills/pr-pilot ~/.bob/skills/pr-pilot
```

### 3. Configure GitHub Actions (recommended for CI)

Copy the workflow file into your repository:

```bash
cp .github/workflows/pr-pilot.yml /path/to/your/repo/.github/workflows/
```

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

| Secret | Description |
|--------|-------------|
| `BOB_API_KEY` | IBM Bob API key |

The `GITHUB_TOKEN` secret is automatically provided by GitHub Actions.

### 4. Configure environment variables (for webhook server)

```bash
cd server
cp .env.example .env
# Edit .env with your values
```

Required variables:

```env
GITHUB_TOKEN=ghp_...              # GitHub token with repo scope
GITHUB_REPO=owner/repo            # Your repository
GITHUB_WEBHOOK_SECRET=...         # Secret from GitHub webhook settings
```

### 5. Install webhook server dependencies

```bash
cd server
npm install
```

---

## Running

### Option A — GitHub Actions (recommended)

The workflow in `.github/workflows/pr-pilot.yml` triggers automatically on PR open/update. No manual steps needed after setup.

### Option B — Webhook server (self-hosted)

```bash
cd server
npm start
```

Configure GitHub to send webhooks to `http://your-server:3000/webhook`.

Set the webhook content type to `application/json` and add the same `GITHUB_WEBHOOK_SECRET` you configured in `.env`.

### Option C — Manual / local (for testing)

```bash
export GITHUB_TOKEN=ghp_...
export GITHUB_REPO=owner/repo

./scripts/review.sh <pr_number> <base_sha> <head_sha>
# Example:
./scripts/review.sh 42 abc1234 def5678
```

---

## Running the sample Spring Boot app

```bash
cd sample-repo

# Start MySQL (or use the H2 in-memory config for tests)
# Update src/main/resources/application.properties with your DB credentials

mvn spring-boot:run
# App runs at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Running tests

```bash
cd sample-repo
mvn test
```

---

## Running server tests

```bash
cd server
npm test
```

---

## How it works

```
GitHub PR opened / updated
        │
        ▼
GitHub Actions workflow OR webhook server
        │
        ▼
 review.sh  ─────────────────────────────────────────────────────────┐
  1. git diff → .pr-pilot-work/pr-<N>.diff                           │
  2. Fetch PR title + description via GitHub API                      │
  3. Build prompt from prompts/main-agent.md                         │
  4. bob --chat-mode=pr-reviewer --print "<prompt>"                  │
        │                                                             │
        ▼                                                             │
  IBM Bob 2.0 (pr-reviewer mode)                                     │
  1. Activates pr-pilot skill                                        │
  2. Reads design doc (if provided)                                  │
  3. Spawns 5 subagents IN PARALLEL:                                 │
     ├── Logic subagent                                              │
     ├── Test coverage subagent                                      │
     ├── Security subagent                                           │
     ├── Style subagent                                              │
     └── Docs subagent                                               │
  4. Merges findings into structured Markdown report                 │
        │                                                             │
        ▼                                                             │
  post-comment.sh                                                     │
  Posts review as GitHub PR comment ◄──────────────────────────────-─┘
```

---

## Customising for your team

### Updating team conventions

Edit `.bob/skills/pr-pilot/conventions.md` to reflect your team's:
- Package naming patterns
- Architecture rules
- REST API conventions
- Test naming conventions

### Updating security rules

Edit `.bob/skills/pr-pilot/security-checklist.md` to add or remove checks.

### Adjusting severity

Edit `.bob/skills/pr-pilot/severity-guide.md` to change what counts as CRITICAL vs WARNING.

### Blocking merges on critical findings

`review.sh` exits with code `1` when critical findings are detected. The GitHub Actions workflow reports this as a failed check, which blocks merging when branch protection rules require all checks to pass.

To enable merge blocking:
1. Go to your repository's Settings → Branches
2. Add a branch protection rule for `main`
3. Enable "Require status checks to pass before merging"
4. Add "PR Pilot Review" as a required status check

---

## Security

- GitHub webhook payloads are verified with HMAC-SHA256 (`X-Hub-Signature-256`) before processing.
- `GITHUB_TOKEN`, `GITHUB_WEBHOOK_SECRET`, and `BOB_API_KEY` are stored as GitHub Secrets or `.env` (never committed).
- The `pr-reviewer` Bob mode does not include the `edit` group — Bob cannot modify files during review without explicit human approval.

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-improvement`
3. Commit your changes following the [Conventional Commits](https://www.conventionalcommits.org/) spec
4. Open a PR — PR Pilot will review it automatically 🤖
