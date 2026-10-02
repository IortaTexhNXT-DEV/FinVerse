# Deployment on Amazon EKS

Audience: BDO IT platform, network and security teams, and the release team. Design:
[`ARCHITECTURE_OPTION_DECISION.md`](../architecture/ARCHITECTURE_OPTION_DECISION.md) sections 2 and 5 (confirmed by
BDOI on 26-Sep-2026). Settings: [`CONFIGURATION.md`](CONFIGURATION.md). Day-to-day operation:
[`RUNBOOK.md`](RUNBOOK.md).

## 1. Workloads

One backend image (`bibs-backend`) runs as three deployments, each with its runtime role
(`BROKERVERSE_RUNTIME_ROLE`), plus the frontend image (`bibs-frontend`).

| Deployment | Role | Serves | Replicas | Requests / limits | Scaling and availability |
|---|---|---|---|---|---|
| `bibs-web` | `web` | Screens and user APIs (`/api/**`); no schedulers, no Kafka consumers, no outbox relay | UAT 2-4, prod 3-10 | 1 CPU, 2 GiB / 2 CPU, 3 GiB | HPA on CPU (65 %) and memory (80 %); PDB `minAvailable: 1`; spread across zones |
| `bibs-jobs` | `jobs` | Scheduled and batch jobs (month-end, remittance and renewal extraction, migration loads, reports); HTTP limited to the actuator | UAT 1, prod 2 | 1 CPU, 3 GiB / 2 CPU, 4 GiB (UAT 0.5 CPU, 2 GiB / 2 CPU, 3 GiB) | Fixed; the Redis job lock runs each job once; PDB `maxUnavailable: 1`; 120 s grace period |
| `bibs-integration` | `integration` | Outbox relay to MSK, Kafka consumers, inbound bank / insurer / watchlist files, `/integration/**` (Apigee X) | 2 | 0.5 CPU, 1.5 GiB / 2 CPU, 2 GiB | Fixed; PDB `minAvailable: 1`; spread across zones |
| `bibs-frontend` | – | nginx serving the React build over TLS | 2-4 | 50m, 64 MiB / 0.5 CPU, 256 MiB | HPA on CPU (70 %); PDB `minAvailable: 1`; spread across zones |

All pods: HTTPS on 8443, startup / readiness / liveness probes over HTTPS (`/livez`, `/readyz` on the backend,
`/healthz` on the frontend),
rolling updates with `maxUnavailable: 0`, a `preStop` pause so the load balancer deregisters a pod before it stops.
Every pod runs as a non-root user (backend uid/gid 10001, frontend nginx uid/gid 101) with a read-only root file
system, no Linux capabilities, no privilege escalation and the `RuntimeDefault` seccomp profile; the namespace
enforces the `restricted` Pod Security level. Writable paths are `emptyDir` volumes: `/tmp` for the backend; the
rendered configuration (`/etc/nginx/conf.d`), `/var/cache/nginx`, `/var/run` and `/tmp` for nginx. The backend JVM
takes 75 % of the container memory (`JAVA_OPTS`).

Every container declares CPU, memory and ephemeral-storage requests and limits (the ephemeral-storage limit covers
the `emptyDir` volumes); the `LimitRange` `bibs-container-defaults` bounds any other container of the namespace. The
PodDisruptionBudgets use `unhealthyPodEvictionPolicy: AlwaysAllow`, so a pod that is not ready never blocks a node
drain. The service account token is mounted only on the three backend accounts that use IRSA.

Container images: the frontend runtime image is `nginxinc/nginx-unprivileged` (stable line) listening on 8080
(plain HTTP, docker compose) and 8443 (TLS, Kubernetes); the backend runs on the Eclipse Temurin 21 JRE. Every base
image is pinned by digest in the Dockerfiles; the header comment of each Dockerfile says how to refresh a pin. The
pipeline scans both built images (Trivy) before they are pushed.

## 2. Manifests (`deploy/k8s`, Kustomize)

| Path | Content |
|---|---|
| `base/` | Namespace `bibs`, service accounts (IRSA), `bibs-backend-config`, the four deployments and their services, HPAs, PodDisruptionBudgets, NetworkPolicies, cert-manager certificates, the ALB ingresses; includes `deploy/nginx` (TLS configuration of nginx as a ConfigMap) |
| `components/environment/` | Copies the values of an overlay's `environment.yaml` (host, ARNs, network ranges) into the base |
| `overlays/uat/`, `overlays/prod/` | Image names and tags, `environment.yaml`, profile and sizing patches |
| `base/secrets.example.yaml` | Template of the secret `bibs-backend-secrets` (never committed with values) |

Build and apply:

```bash
kubectl kustomize deploy/k8s/overlays/prod          # review the output
kubectl apply -k deploy/k8s/overlays/prod
```

Both overlays build with `kubectl kustomize` (Kustomize 5) and the output validates against the Kubernetes 1.30
schemas and the cert-manager / AWS Private CA issuer schemas (`kubeconform -strict`). The manifests use the
`preStop` sleep action (Kubernetes 1.30 or later).

Prerequisites on the cluster: AWS Load Balancer Controller, cert-manager with the AWS Private CA issuer plugin,
metrics server, NetworkPolicy enforcement (VPC CNI network policy agent or Calico), and the ConfigMap with the RDS
CA bundle:

```bash
kubectl -n bibs create configmap rds-ca-bundle --from-file=global-bundle.pem   # AWS RDS global CA bundle
```

The secret `bibs-backend-secrets` is created from AWS Secrets Manager (External Secrets Operator or the release
pipeline) with the keys of `base/secrets.example.yaml`.

## 3. Edge

- **Users**: BDO network (Direct Connect / VPN, VDI) → Route 53 private zone → AWS WAF → **internal** ALB (ingress
  `bibs-users`): `/api/*` → `bibs-web`, `/*` → `bibs-frontend`. HTTPS 443 only, ACM certificate on the BDO domain,
  policy `ELBSecurityPolicy-TLS13-1-2-2021-06`, invalid header fields dropped, `inbound-cidrs` limited to the BDO
  client networks and the Apigee route.
- **Systems**: BDO systems → Apigee X → private route → the same ALB (ingress `bibs-integration`, evaluated first):
  `/integration/*` → `bibs-integration`. BIBS validates the Apigee token (issuer, audience, scopes per API; see
  CONFIGURATION.md). The WAF web ACL admits `/integration/*` only from the Apigee source ranges.
- The WAF web ACL carries the AWS managed rule groups (core rule set, known bad inputs, SQL injection) and a
  rate-based rule on `/api/v1/auth/*`, on top of the in-application login rate limit.
- **Upload size.** BIBS accepts uploads up to 25 MB through the application (`spring.servlet.multipart`, 26 MB
  request) and larger inbound files by presigned upload straight to S3. The core rule set's `SizeRestrictions_BODY`
  rule blocks request bodies over 8 KB, so set it to Count (or scope it down) for the multipart upload paths
  (`/api/v1/**` requests with `Content-Type: multipart/form-data`) and keep a custom size rule of 26 MB there. The ALB
  itself has no body-size limit.
- Security headers (CSP, HSTS, frame options, referrer policy) come from nginx for the SPA and from the backend
  for the APIs.

## 4. Encryption in transit and network

| Hop | Control |
|---|---|
| ALB → pods | `backend-protocol: HTTPS`; certificates `bibs-*-tls` from AWS Private CA through cert-manager (90 days, renewed 15 days before expiry, new key each time). The backend reloads a renewed certificate by itself; nginx picks it up on its next restart (`kubectl -n bibs rollout restart deploy/bibs-frontend` after a renewal, or a config reloader) |
| Backend → RDS PostgreSQL | `sslmode=verify-full` with the RDS CA bundle; RDS parameter `rds.force_ssl=1` |
| Backend → ElastiCache Redis | in-transit encryption, AUTH token or RBAC user |
| Backend → MSK | `SASL_SSL` with SCRAM-SHA-512 (port 9096) or IAM (9098); plaintext listeners disabled on the cluster |
| Backend → S3, KMS, STS, Secrets Manager | interface VPC endpoints over TLS; bucket policies deny non-TLS requests |
| Backend → BDO systems | through Apigee X over TLS; SMTP to CCM with STARTTLS |

A production start is refused when any of these connections could run in plaintext (CONFIGURATION.md "Production
start-up safeguards").

NetworkPolicies deny all traffic by default. Allowed: DNS; load balancer subnets → `bibs-web`, `bibs-integration`,
`bibs-frontend` on 8443; namespace `monitoring` → backend pods on 8443 (metrics); backend → data subnets (5432,
6379, 9096, 9098); backend → VPC endpoint subnets (443); backend → BDO private route (443, 587). Pods never call each
other; `bibs-frontend` has no egress besides DNS. Kubelet probes are not affected by the policies.

Each backend deployment has its own service account with an IAM role (IRSA); no AWS keys are stored anywhere.

## 4a. Database roles

The schema owner and the application login are separate (least privilege):

| Login / role | Created by | Rights | Used by |
|---|---|---|---|
| `bibs_owner` (schema owner, `SPRING_FLYWAY_USER`) | DBA | owner of every table, sequence and function of the schema; `CREATEROLE` only if it is to create the runtime role itself | Flyway at start-up (all DDL, grants) |
| `brokerverse_runtime` (`BROKERVERSE_DB_RUNTIME_ROLE`, `NOLOGIN`) | migration V1190 (or the DBA beforehand) | `SELECT`, `INSERT`, `UPDATE`, `DELETE` on the tables, `USAGE`/`SELECT` on the sequences, read-only on `flyway_schema_history`; no `CREATE`, `TRUNCATE`, `TRIGGER` or `REFERENCES`, owner of nothing | – (group role) |
| `bibs_app` (`BROKERVERSE_DB_USER`) | DBA | `LOGIN`, member of `brokerverse_runtime` | the application connection pool |

The runtime login therefore cannot change the schema, truncate a table, disable the immutability triggers of the
audit and ledger tables (only the owner can) or rewrite the migration history. The default privileges of V1190 give
the runtime role the same row access on every table a later migration creates. A production start is refused when
`SPRING_FLYWAY_USER` is missing or equals `BROKERVERSE_DB_USER` (CONFIGURATION.md "Production start-up safeguards").

One-time set-up by the DBA on Amazon RDS (as the master user; passwords from AWS Secrets Manager, never in scripts):

```sql
CREATE ROLE bibs_owner LOGIN PASSWORD '<from Secrets Manager>';
CREATE ROLE brokerverse_runtime NOLOGIN;
CREATE ROLE bibs_app LOGIN PASSWORD '<from Secrets Manager>' IN ROLE brokerverse_runtime;
CREATE DATABASE brokerverse OWNER bibs_owner;
\c brokerverse
ALTER SCHEMA public OWNER TO bibs_owner;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
```

An existing database whose objects belong to the former application login moves to the owner with
`REASSIGN OWNED BY <former login> TO bibs_owner;` (run in the database, as the master user) before the release
that sets `SPRING_FLYWAY_USER`; V1190 then grants the runtime role on the existing objects. Every backend pod runs
Flyway at start-up (a lock serialises them), so all three deployments carry the owner credentials in
`bibs-backend-secrets`; the migration connection uses the same TLS settings as the pool.

Development, the automated tests and the docker compose stack use one login (the owner) for both; V1190 still
creates the runtime role there, and `DatabaseRuntimeRoleIT` proves what the role cannot do.

## 5. Values BDO IT provides

Set in `overlays/<env>/environment.yaml` and the `bibs-backend-config` patch of the overlay:

| Value | Owner |
|---|---|
| Host name on the BDO domain and its ACM certificate (issued under the BDO corporate PKI, or by ACM if the PKI team allows) | BDO PKI / network team |
| WAF web ACL (managed rule groups, rate rule, Apigee-only rule for `/integration/*`) | BDO security team |
| Client network ranges and the Apigee X source ranges of the private route (HA VPN or Interconnect / Direct Connect) | BDO network team |
| Subnet ranges of the ALB, the data services and the VPC endpoints | BDO cloud platform team |
| AWS Private CA for in-cluster certificates, IAM roles of the three backend service accounts | BDO cloud platform / security teams |
| Apigee X key set address, token issuer and audience; the scopes per published API | BDO API team |
| ECR registry and release tags; secrets in AWS Secrets Manager | Release team |
| Database logins: schema owner and least-privilege application login (section 4a) | DBA |

Every value that is still a `<...>` placeholder must be replaced before the first apply: the base keeps
documentation-only network ranges (`192.0.2.x`) that match nothing, so a forgotten range blocks traffic instead of
opening it.

## 6. Monitoring

The actuator (`/actuator/health`, `/actuator/prometheus`) answers on every role on the management port 9090
(`BROKERVERSE_MANAGEMENT_PORT`), which the load balancer never publishes; the NetworkPolicy `allow-monitoring`
admits only the `monitoring` namespace to it, so Prometheus scrapes without a user token (pod annotations
`prometheus.io/scrape`, `port`, `path`). The probes and the load balancer use `/livez` and `/readyz` on 8443. Each deployment is sized and monitored separately; the job monitor
(*Administration › Scheduled Jobs*) shows the runs of `bibs-jobs` and `bibs-integration`.
