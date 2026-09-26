# Application architecture: IER picture or BIBS as built

Status: **Recommendation, for BDOI confirmation** (IER question IQ25, register DCR-222 / DCR-223). Date: 26-Sep-2026.

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

## 4. Decision needed from BDOI

1. Confirm the BIBS architecture, with the three elements taken from the IER.
2. The edge standard: ALB and WAF, or Apigee X for inter-system APIs.
3. Whether BDO security requires mTLS inside the cluster.
