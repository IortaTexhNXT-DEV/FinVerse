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
