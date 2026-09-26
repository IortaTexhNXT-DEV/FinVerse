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

All pods: HTTPS on 8443, startup / readiness / liveness probes over HTTPS (`/actuator/health/*`, `/healthz`),
rolling updates with `maxUnavailable: 0`, a `preStop` pause so the load balancer deregisters a pod before it stops.
Backend pods run as a non-root user with a read-only root file system, no Linux capabilities and the
`RuntimeDefault` seccomp profile. The backend JVM takes 75 % of the container memory (`JAVA_OPTS`). The frontend
image runs nginx as root today, so the namespace enforces the `baseline` Pod Security level (and warns on
`restricted`); an unprivileged nginx base image for the frontend would allow `restricted` for the whole namespace.

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

Every value that is still a `<...>` placeholder must be replaced before the first apply: the base keeps
documentation-only network ranges (`192.0.2.x`) that match nothing, so a forgotten range blocks traffic instead of
opening it.

## 6. Monitoring

The actuator (`/actuator/health`, `/actuator/prometheus`) answers on every role over HTTPS on 8443; Prometheus
scrapes from the `monitoring` namespace. Each deployment is sized and monitored separately; the job monitor
(*Administration › Scheduled Jobs*) shows the runs of `bibs-jobs` and `bibs-integration`.
