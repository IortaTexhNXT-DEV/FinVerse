# Quality Gates (SonarQube-equivalent)

Every build (`mvn verify`, `npm run verify`) and every CI run enforces the gates below. A change
cannot be merged unless all pass. No rule may be weakened or suppressed without a written
justification next to the exclusion (see `backend/quality/*.xml`).

## Backend (Java)

| Gate | Tool | Covers Sonar categories |
|---|---|---|
| Formatting | Spotless + google-java-format | Code style |
| Compiler warnings are errors | `javac -Xlint:all -Werror` | Bugs |
| Coding standard | Checkstyle (`quality/checkstyle.xml`) | Naming, size, complexity, magic numbers, Javadoc |
| Code smells & error-prone code | PMD 7 (`quality/pmd-ruleset.xml`) | Code smells, bugs, performance, multithreading, security, cognitive complexity |
| Duplication | PMD CPD (≥ 120 tokens) | Duplications |
| Bugs & vulnerabilities | SpotBugs (max effort, low threshold) + FindSecBugs | Bugs, vulnerabilities, security hotspots |
| Architecture | ArchUnit (`ArchitectureTest`) | Design: layering, no module cycles |
| Tests | JUnit 5, integration tests on real PostgreSQL | Reliability |
| Coverage | JaCoCo ≥ 80 % lines, ≥ 65 % branches | Coverage |

## Frontend (TypeScript)

| Gate | Tool |
|---|---|
| SonarSource rules (the same rule engine SonarQube uses for JS/TS) | `eslint-plugin-sonarjs` recommended |
| Type safety | TypeScript strict + `typescript-eslint` strict-type-checked |
| Accessibility | `eslint-plugin-jsx-a11y` strict |
| React correctness | `eslint-plugin-react-hooks` |
| Complexity | cognitive ≤ 15, cyclomatic ≤ 12, max depth 3, max 400 lines/file |
| Formatting | Prettier |
| Tests | Vitest + Testing Library |

## Real SonarQube / SonarCloud

`backend/pom.xml` and `frontend/sonar-project.properties` carry the project keys. CI runs the scanner
automatically when the CI variable `SONAR_TOKEN` (and optionally variable `SONAR_HOST_URL`)
is set, and waits for the SonarQube quality gate.

The CI pipeline also runs static application security testing (SAST) of the Java and TypeScript code on every
pipeline and weekly (`.gitlab-ci.yml`, stage `security`).

## Dependency and supply-chain checks

Both pipeline definitions (`.gitlab-ci.yml` and the workflow files under `.github/workflows`) run the same
security gates on every pipeline, including merge requests:

| Check | Tool | Fails on | Accepted findings |
|---|---|---|---|
| Java dependencies | OWASP dependency-check (`mvn org.owasp:dependency-check-maven:check`, NVD data, key in the masked variable `NVD_API_KEY`) | CVSS 7.0 or higher | `backend/quality/dependency-check-suppressions.xml` |
| Web client dependencies | `npm audit --audit-level=high` (the install no longer hides the audit with `--no-audit`) | high or critical advisory | none; upgrade the package or pin a fixed version with `overrides` in `package.json` |
| Container images | Trivy on the built backend and frontend images (OS packages, the application jar, secrets) | a high or critical vulnerability with a fix available | `.trivyignore.yaml` |
| Secrets | gitleaks over the whole history | any finding | `.gitleaksignore` (fingerprints) |
| Code | CodeQL (Java, TypeScript) on pushes and merge requests, GitLab SAST | as configured in the code scanning service | – |
| GitLab security templates | Dependency-Scanning, Container-Scanning, Secret-Detection | reported in the security dashboard | – |

**Software bill of materials.** `mvn package` writes the CycloneDX SBOM of the backend runtime
dependencies to `backend/target/bom.json` (and `bom.xml`); the frontend job writes
`frontend/sbom-frontend.cdx.json` (`npm sbom`, runtime dependencies). Both are kept as build artefacts of every
pipeline and belong to the release record.

**Rule for findings.** A finding is fixed by upgrading the library, the pinned base image or the package. An
accepted finding is the exception: every entry names the reason it does not apply, the reviewer and the review
date, and carries an expiry date at most six months ahead (`until` in the dependency-check file, `expired_at` in
the Trivy file, a review date in the gitleaks file); after the expiry the gate fails again.

**Pinning.** Pipeline actions are pinned by the commit SHA of their release and the base images of both
Dockerfiles by digest; the scanners are downloaded as release archives and verified against their SHA-256. A
weekly update proposal (`.github/dependabot.yml`) covers actions, base images, Maven and npm; the Dockerfile header
says how to refresh a digest by hand.
