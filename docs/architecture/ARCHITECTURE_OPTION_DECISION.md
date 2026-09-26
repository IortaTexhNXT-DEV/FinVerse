# Application architecture: IER picture or BIBS as built

Status: **Architecture confirmed by BDOI, 26-Sep-2026** (the BIBS architecture with the three IER elements). The edge and in-cluster
encryption proposals are in section 5 (IER question IQ25, register DCR-222 / DCR-223).

## 1. The two options

| | IER picture ("Technical Architecture - Multi-Tenant") | BIBS as built |
|---|---|---|
| Shape | About ten microservices (policy admin, claims, billing and collections, distribution, portal, API gateway, IAM, integration, document, batch and notification) across three tenants | One modular monolith (Spring Boot 3.5, Java 21) with about 40 business modules, plus one React front end |
| Data | Aurora and MongoDB, one database per service, "no cross-database transactions" | One PostgreSQL 16 database. A business record, its journal, ledger and sub-ledger rows commit in **one transaction** |
| Messaging | EventBridge plus MSK, about 80 topics, schema registry | MSK Kafka for outbound integration events (9 topics, transactional outbox); in-process events between modules |
| Edge and mesh | CloudFront, WAF, Apigee X, Istio with mTLS and OPA per pod | Ingress to nginx and the backend; JWT issued by BIBS |
| Tenancy | Multi-tenant (broker, EB insurer portal, RI network) | Single tenant (BDOI) with multi-company support |

## 2. Recommendation: the BIBS architecture, with three enterprise elements taken from the IER

**BIBS as built is the better practice for BDOI.**

- **Financial consistency.** A premium, its receivable, the remittance to the insurer, the commission and the GL entries must
  agree to the centavo. BIBS posts them in one database transaction. Split across services with separate databases,
  the same flow needs sagas, compensations and reconciliation jobs to reach the same result, with new failure modes
  (half-posted receipts, ledgers out of balance) that production support would have to chase.
- **Scale.** BDOI's volumes (1,344 named users, 429 concurrent at peak per the Core Replacement BRD, retail transaction
  volumes, month-end batches) are well within one horizontally scaled application on RDS Multi-AZ. Microservices pay off
  when separate teams release separate services at different speeds, or parts need very different scaling. Neither
  applies here.
- **Production support and cost.**
  - One codebase, one deployment and one log trail with correlation ids are easier to operate.
  - One database is simpler to back up, restore, replicate for DR and reconcile.
  - Istio, OPA, EventBridge, MongoDB and ten pipelines would add licences, infrastructure and skills without a business
    need in the BRDs.
- **Room to grow.** The modules are separated by enforced boundaries (ArchUnit rules, service ports, events), so a module
  can later be deployed on its own if a real need appears. Candidates are document generation, report rendering and
  integration adapters. That step is cheap because the boundaries already exist.

**Taken from the IER, because they are enterprise standards and do not change the application design:**
1. **Edge security:** Route 53, AWS WAF and the Application Load Balancer in front of the ingress, or Apigee X if BDO
   mandates it for APIs exposed to other BDO systems. BIBS does not need Istio or OPA: authorisation is done in the
   application per permission, and TLS runs from the load balancer into the cluster (in-cluster mTLS only if BDO
   security requires it).
2. **Managed AWS services:** EKS, RDS PostgreSQL 16 Multi-AZ, ElastiCache Redis 7, MSK Kafka, S3 with KMS
   (`DOCUMENT_STORAGE_DECISION.md`), CloudWatch or the BDO monitoring standard.
3. **Workload separation on Kubernetes** (best practice for a modular monolith). The same image runs as three
   deployments, each scaled on its own:

| Deployment | Role | Scaling |
|---|---|---|
| `bibs-web` | Screens and APIs for users | Horizontal pod autoscaler on CPU and requests; at least 2 replicas across availability zones |
| `bibs-jobs` | Scheduled and batch jobs: month-end, remittance extraction, renewal extraction, migration loads, report generation | 1-2 replicas; the Redis job lock already ensures a job runs once; heavy windows scaled on schedule |
| `bibs-integration` | Kafka outbox relay and consumers, inbound bank and insurer files, ECM archiving | 1-2 replicas |
| `bibs-frontend` | nginx serving the React build | 2 replicas |

Batch load then never slows the screens, and each workload is sized and monitored separately. It needs a small build
change: a switch that turns scheduling and consumers on or off per deployment (the application already enables
scheduling globally in `config/ApplicationConfig.java`).

## 3. What changes

| Area | Change |
|---|---|
| IER workbook | The architecture sheets describe BIBS as in section 2; the Kubernetes sheets list the four deployments above with their sizing; MongoDB, Aurora, EventBridge, Istio and OPA rows are removed; the EFS formula is corrected |
| Build | Workload-role switch (`brokerverse.runtime.role` = web / jobs / integration / all) controlling scheduling and Kafka consumers; Kubernetes manifests and Helm values for the four deployments |
| Documents | Technical and deployment architecture (deliverable 12) and the BOM (deliverable 4) follow this decision |

## 4. BDOI confirmation (26-Sep-2026)

BDOI confirmed the BIBS architecture with the three elements taken from the IER. BDOI uses Apigee X across its internal
landscape and asked for best-practice proposals on the edge and on encryption inside the cluster (section 5).

## 5. Proposals: edge and in-cluster encryption

### 5.1 Edge: two separate paths

Users and systems enter BIBS through different paths. Each path gets the control that suits it, and neither pays for
the other's.

| Path | Route | Controls |
|---|---|---|
| **Users (browser)** | BDO network (Direct Connect / VPN, VDI) → Route 53 private zone → **AWS WAF** → **internal Application Load Balancer** (AWS Load Balancer Controller) → `bibs-frontend` (nginx) and `/api` → `bibs-web` | ALB is **internal** (no public exposure); TLS 1.2+ with an ACM certificate on the BDO domain. WAF uses the AWS managed rule groups (core rule set, known bad inputs, SQL injection) plus a rate-based rule on `/api/v1/auth/*` on top of the in-app login rate limit. Security headers (CSP, HSTS, frame options) come from nginx. Sign-in is through EIAM (Entra ID) once IQ04 is settled |
| **Other BDO systems (APIs)** | BDO systems → **Apigee X** (BDO's standard) → private connectivity → internal ALB, path `/integration/*` → `bibs-integration` / `bibs-web` | Each BIBS API used by another system (EGL, EDP, CMS / New BOB, OBPCS, PMS and the rest) is published as an Apigee proxy with OAuth 2.0 client credentials, quotas, spike arrest and analytics. BIBS checks the Apigee-issued token (audience and scopes) and accepts `/integration/*` only from the Apigee path, enforced by a WAF rule and a security group. **Outbound** calls from BIBS to BDO systems also go through Apigee, so BDO IT governs every interface in one place. Asynchronous events stay on MSK Kafka |

Apigee is **not** placed in the user path: it would add latency and cost to every screen action and gives nothing that
WAF and the ALB do not already give for browser traffic. Using ALB and WAF for users and Apigee for systems follows
both AWS practice and BDO's own API governance.

### 5.2 Encryption inside the cluster: TLS on every hop, no service mesh

BIBS has four deployments, and almost all its internal traffic goes to AWS managed services. East-west traffic between
pods is limited to the load balancer reaching the frontend and backend. The proposal is **encrypt every hop with
TLS, lock down pod-to-pod traffic with network policies, and use no service mesh**:

| Hop | Control |
|---|---|
| ALB → pods | HTTPS to the pods (target-group protocol HTTPS); certificates from cert-manager with a private CA (ACM Private CA), rotated automatically. The backend serves TLS (`server.ssl`), nginx serves TLS |
| Backend → RDS PostgreSQL | TLS required: RDS parameter `rds.force_ssl=1`; JDBC `sslmode=verify-full` with the RDS CA bundle |
| Backend → ElastiCache Redis | In-transit encryption on, Redis AUTH or RBAC user (`BROKERVERSE_REDIS_TLS=true`, already supported) |
| Backend → MSK Kafka | TLS with SASL/SCRAM or IAM authentication (`BROKERVERSE_KAFKA_SECURITY_PROTOCOL=SASL_SSL`, already supported); plaintext listeners disabled on the cluster |
| Backend → S3, KMS, other AWS APIs | TLS through VPC endpoints; bucket policies deny non-TLS requests |
| Backend → BDO systems | Through Apigee over TLS (5.1); SMTP to CCM with STARTTLS (already configured) |
| Pod to pod | Kubernetes **NetworkPolicies**, deny by default: only ingress → frontend and backend, and backend → the data services. EKS security groups for pods; workload identities (IRSA) with least-privilege IAM |

**Why no mesh.** A service mesh (Istio or similar) is worth it with many services calling each other. BIBS has almost
no pod-to-pod calls, so a mesh would add sidecars, certificates to operate, upgrades and skills for little security
gain. The controls above already give encryption in transit end to end, and least-privilege network and identity
controls. If BDO security later mandates mTLS between every pod as a zero-trust rule, a sidecar-less mesh (for example
Istio ambient mode) can be added at the platform layer without any change to BIBS.

### 5.3 Build items (INF0)

| Item | Where |
|---|---|
| Workload role switch `brokerverse.runtime.role` (web / jobs / integration / all) controlling scheduling and Kafka consumers | `config/**`, jobs and Kafka consumer configuration |
| Backend HTTPS (`server.ssl` from a mounted certificate), nginx TLS listener | `application.yml`, `deploy/nginx` |
| Database TLS in the documented JDBC URL; Redis and Kafka TLS on by default outside `dev` / `test` | `application.yml`, CONFIGURATION.md |
| Apigee token validation for `/integration/*` (issuer, audience, scopes) as a separate security chain | `security/**` |
| Kubernetes manifests / Helm values: four deployments, HPA, PodDisruptionBudgets, NetworkPolicies (deny by default), IRSA service accounts, ALB ingress annotations (internal, HTTPS backend, WAF ACL) | `deploy/k8s/**` |
| Documentation: technical and deployment architecture (deliverable 12), BOM (deliverable 4), IER restatement | docs |

### 5.4 Points for BDO IT

1. Confirm the two-path edge (ALB and WAF for users; Apigee X for systems, inbound and outbound).
2. Confirm TLS on every hop with network policies and no mesh; or state if a zero-trust mTLS mandate applies.
3. The private connectivity between Apigee X and the BIBS VPC (Private Service Connect, VPN or Interconnect), and the certificate authority to use (ACM Private CA or BDO PKI).
