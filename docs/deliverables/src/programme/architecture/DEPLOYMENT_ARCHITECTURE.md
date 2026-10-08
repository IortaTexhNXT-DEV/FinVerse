---
# Deployment Architecture of BIBS (BRD-00, Programme/Architecture).
# Build: python docs/deliverables/src/programme/architecture/build_architecture_pack.py
title: Deployment Architecture
subtitle: Runtime components, Kubernetes topology per environment, Gateway API routing, configuration and the release pipeline of BIBS
doc_type: Deployment Architecture
doc_code: Architecture
brd: BRD-00
name: Deployment Architecture
doc_id: BIBS-ARC-02
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Architecture set, document 2 of 6
h1_page_break: false
output: Architecture/BIBS_Architecture_BRD-00_Deployment_Architecture_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Tech Lead; iorta TechNXT DevOps Engineer
    approver: BDOI IT Cloud and Infrastructure (pending)
    change: First issue, with the Kubernetes Gateway API edge and the Valkey baseline of 8 October 2026
distribution:
  - {name: "Cloud and Digital Operations Engineering", role: Approver, organisation: BDO Unibank IT, purpose: "Clusters, environments, release pipeline"}
  - {name: "Enterprise Architecture", role: Reviewer, organisation: BDOI IT, purpose: "Deployment model"}
  - {name: "Information Security Office", role: Reviewer, organisation: BDOI IT, purpose: "Secrets, network policies, edge"}
  - {name: "Network team", role: Reviewer, organisation: BDO Unibank IT, purpose: "Gateway, source ranges, private route"}
  - {name: "IT Operations", role: Informed, organisation: BDOI IT, purpose: "Promotion and production support"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Manifests, pipeline, releases"}
---

# Introduction

## Purpose and audience

This document describes how BIBS is packaged, deployed, configured, scaled and released on Amazon EKS, for BDO IT Cloud and Digital Operations Engineering, the network and security teams and the release team. It turns the application architecture of the Solution Architecture into the runtime components of each environment, and it sets the rules for promoting one release through SIT, UAT, Pre-Prod, production and DR. The infrastructure under the clusters (network zones, data services, sizing, backups, monitoring) is in the Infrastructure Architecture.

## Scope

All environments of BIBS (DEV, SIT, UAT, Pre-Prod, PROD and DR), the two container images, the four workloads, the Kubernetes resources of the namespace bibs, the edge (Gateway API, AWS WAF, internal load balancer), configuration and secrets, and the release pipeline. The CI/CD toolchain itself is BDO's (IQ29); this document states what the pipeline must do.

## Sources

<!-- table: widths=1,7,9.6 caption="Sources" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| S1 | Application architecture option decision (confirmed 26-Sep-2026), sections 2 and 5 | Workloads, edge, encryption in the cluster |
| S2 | BIBS deployment guide, configuration reference and runbook | Manifests, variables, probes, start-up safeguards, release rules |
| S3 | Client decisions of 8-Oct-2026 | Gateway API instead of ingress-nginx, Valkey instead of Redis, component baseline |
| S4 | IER workbook v20 (Kubernetes sheets, HW and SW sheet, assumptions) | Environments, operating hours, cluster add-ons, growth |
| S5 | Quality gates and security controls | Pipeline stages and security gates |
| S6 | Programme Alignment pack v1.0 (timeline, IQ25 to IQ35) | Environment dates, open questions |

## Conventions

Open points of this document are numbered DA-01 to DA-08 (chapter 10). Resource names are written as in the manifests (bibs-web, bibs-users). Times are Philippine time unless marked UTC.

# Deployment principles

<!-- table: widths=1,4.4,12.2 caption="Deployment principles" bold=first size=8.5 -->
| # | Principle | Practice |
|---|---|---|
| D1 | Package once, promote by digest | The pipeline produces each image once per release; SIT, UAT, Pre-Prod, PROD and DR run the same image digest; only the environment overlay differs |
| D2 | Everything declarative | All Kubernetes resources come from Kustomize: one base and one overlay per environment; no manual change on a cluster survives the next apply |
| D3 | Immutable, minimal, scanned images | Pinned base images by digest; non-root users; read-only root file systems; Trivy scan and SBOM before push |
| D4 | Zero-downtime changes | Rolling updates with no unavailable pod, disruption budgets, a pre-stop pause, and schema changes that are backward compatible with the previous release |
| D5 | Least privilege | One service account and IAM role per workload; network policies deny by default; secrets only from AWS Secrets Manager |
| D6 | Refuse unsafe starts | The backend refuses to start with missing or development secrets, the seed profile in production or any plaintext connection |

# Runtime components and containers

## Runtime view

![Runtime components](figures/da_runtime.dot){width=15.5}

## Container images

<!-- table: widths=2.6,5,5,5 caption="Container images" bold=first size=8.5 -->
| Image | Base and runtime | Runs as | Notes |
|---|---|---|---|
| bibs-backend | Eclipse Temurin 21 JRE on Alpine, pinned by digest; the Spring Boot application archive | User and group 10001, no shell login; JVM limited to 75 % of the container memory, exit on out-of-memory | Ports 8443 (HTTPS) and 9090 (management: health and Prometheus metrics). One image for the three backend workloads |
| bibs-frontend | nginx 1.30 unprivileged on Alpine, pinned by digest; the compiled React application with the BDOI theme pack | nginx user 101 | Ports 8443 (TLS) in Kubernetes and 8080 for single-host stacks; security headers (CSP, HSTS, frame options, referrer policy) |

## Workloads

<!-- arch:workloads -->

All pods serve HTTPS on 8443 with startup, readiness and liveness probes over HTTPS (livez and readyz on the backend, healthz on the frontend), run as non-root users with a read-only root file system, no Linux capabilities, no privilege escalation and the RuntimeDefault seccomp profile; the namespace enforces the restricted Pod Security level. Writable paths are emptyDir volumes (temporary files of the backend, up to 4 GiB for report and extraction files on bibs-jobs). Every container declares CPU, memory and ephemeral-storage requests and limits; a LimitRange bounds any other container of the namespace.

<!-- table: widths=3,3.2,3.2,3.2,5 caption="Probes and shutdown" bold=first size=8.5 -->
| Workload | Startup | Readiness | Liveness | Shutdown |
|---|---|---|---|---|
| bibs-web | livez every 10 s, up to 30 failures (5 minutes for schema migration) | readyz every 10 s, 3 failures | livez every 20 s, 3 failures | Pre-stop pause 15 s so the load balancer deregisters the pod; 45 s grace |
| bibs-jobs | as bibs-web | readyz every 15 s | livez every 30 s, 4 failures | 120 s grace: a running job finishes its unit of work; an interrupted job runs again on its next schedule |
| bibs-integration | as bibs-web | readyz every 10 s, 3 failures | livez every 20 s, 3 failures | Pre-stop pause 15 s; 60 s grace; consumers stop and leave their groups |
| bibs-frontend | - (nginx starts in seconds) | healthz every 10 s | healthz every 20 s | Pre-stop pause 10 s; nginx drains connections |

# Kubernetes topology

## Resources of the namespace bibs

<!-- table: widths=4.2,13.4 caption="Kubernetes resources of BIBS (Kustomize base)" bold=first size=8.5 -->
| Resource | Content |
|---|---|
| Namespace | bibs, Pod Security level restricted |
| Deployments and Services | bibs-web, bibs-jobs, bibs-integration (image bibs-backend, runtime role per deployment), bibs-frontend; one ClusterIP service each on 8443 |
| Horizontal Pod Autoscalers | bibs-web (CPU 65 %, memory 80 %), bibs-frontend (CPU 70 %); bibs-jobs and bibs-integration keep a fixed replica count per overlay |
| Pod disruption budgets | Minimum 1 available for bibs-web, bibs-frontend and bibs-integration; at most 1 unavailable for bibs-jobs; unhealthy pods never block a node drain |
| NetworkPolicies | Deny all by default; open only DNS, load balancer to the three served workloads on 8443, monitoring namespace to the backend metrics on 9090, backend to the data subnets (5432, 6379, 9096, 9098), backend to the PrivateLink interfaces of the AWS services (443), backend to the BDO private route (443, 587) |
| Service accounts | One per workload; the three backend accounts carry an IAM role through IRSA; the frontend gets no token and no AWS access |
| Certificates | cert-manager certificates for the four services from AWS Private CA (90 days, renewed 15 days before expiry, new key each time) |
| Configuration | ConfigMap bibs-backend-config (non-secret settings), secret bibs-backend-secrets (from AWS Secrets Manager), ConfigMap of the RDS CA bundle, nginx TLS configuration |
| Edge | Gateway bibs and the HTTPRoutes bibs-users and bibs-integration (chapter 5) |
| LimitRange | Default requests and limits for any other container of the namespace |

Kustomize assembles them: the base holds the resources above; a component per gateway controller (AWS Load Balancer Controller by default, Envoy Gateway as the alternative); a component copies the values of the environment (host name, certificate and WAF identifiers, network ranges, IAM roles, private CA) into the base; and one overlay per environment sets the images by digest, the profile, the sizing and the gateway component. The base keeps documentation-only network ranges that match nothing, so an overlay that forgets a range stays closed instead of opening traffic.

## Cluster prerequisites

Each EKS cluster needs: the Gateway API standard CRDs (v1) and the gateway controller of its overlay (AWS Load Balancer Controller 2.14 or later with Gateway support); cert-manager with the AWS Private CA issuer; the metrics server; network policy enforcement (VPC CNI network policy support or Calico); the cluster autoscaler (or Karpenter); the External Secrets Operator (DA-02); and the BDO add-ons of the IER sheets (Dynatrace, CrowdStrike Falcon, CoreDNS, kube-proxy, VPC CNI). The EFS CSI controller of the IER sheets is not needed: pods are stateless and files are in S3.

## Topology per environment

<!-- arch:environments -->

<!-- arch:sizing caption="Proposed Kubernetes sizing per environment (pods and nodes)" -->

The node sizes follow from the requests of the workloads. In PROD at the peak of 10 bibs-web pods the four workloads request 13.2 vCPU and about 30 GiB; with the cluster add-ons about 16 vCPU and 34 GiB, which three m5.2xlarge nodes (24 vCPU, 96 GiB) carry at about two thirds of their CPU. The IER asks for three m5.4xlarge nodes (48 vCPU, 192 GiB) in PROD year 1, sized for ten microservices; the smaller nodes are proposed (DA-05) and proved by the performance test on Pre-Prod.

![Production cluster topology](figures/da_topology.dot){width=16}

# Gateway API routing

## Routing

The edge uses the Kubernetes Gateway API (ADR-06). The Kubernetes project retired ingress-nginx in March 2026; BIBS uses neither ingress-nginx nor Ingress resources.

![Edge routing with the Kubernetes Gateway API](figures/da_gateway.dot){width=15}

<!-- table: widths=3.6,4.2,4.2,5.6 caption="Gateway API resources" bold=first size=8.5 -->
| Resource | Rule | Target | Notes |
|---|---|---|---|
| Gateway bibs | Listener https, HTTPS on 443, host name of BIBS, routes from the same namespace only | - | GatewayClass bibs-alb (default) or bibs-envoy, chosen by the overlay |
| HTTPRoute bibs-users | Path prefix api | bibs-web:8443 | Screens' calls to the backend |
| HTTPRoute bibs-users | Path prefix / | bibs-frontend:8443 | The web client |
| HTTPRoute bibs-integration | Path prefix integration | bibs-integration:8443 | System integration through Apigee X only |

The longest matching prefix wins, so the api and integration prefixes never reach the frontend route and no rule ordering is needed. Inside BIBS, the runtime role filter answers 404 for a path that the workload does not serve.

## Load balancer and WAF settings

<!-- table: widths=4.6,13 caption="Settings of the default controller (AWS Load Balancer Controller)" bold=first size=8.5 -->
| Setting | Value |
|---|---|
| Scheme | Internal Application Load Balancer; no public exposure |
| Listener | HTTPS 443 only; ACM certificate on the BDO domain (issued under the BDO corporate PKI or by ACM, DA-03); TLS policy ELBSecurityPolicy-TLS13-1-2-2021-06 |
| Target groups | Target type ip; HTTPS to the pods; health checks readyz (backend) and healthz (frontend) |
| Source ranges | BDO client networks (Direct Connect, VPN, VDI) and the Apigee X private route, one aggregate range each |
| Attributes | Invalid header fields dropped; idle time-out 300 s (long reports and uploads) |
| AWS WAF web ACL | AWS managed rule groups (core rule set, known bad inputs, SQL injection); rate-based rule on the sign-in path on top of the in-application limit (20 per minute per address); the integration prefix admitted only from the Apigee ranges; the body-size rule of the core set counted for multipart uploads and replaced there by a 26 MB rule |
| Upload size | 25 MB per file through the application (26 MB request); larger inbound files by presigned upload straight to S3 (up to 5 GB) |

**Alternative controller.** An environment that cannot use the AWS Load Balancer Controller's Gateway support lists the Envoy Gateway component instead: an internal NLB limited to the same source ranges, the listener certificate from a TLS secret, HTTPS to the pods verified against the private CA, and 300-second route time-outs. An NLB carries no AWS WAF web ACL, so this variant is used only with the agreement of the BDO security team and with the WAF rules applied in front of it.

## Mapping from the former Ingress rules

<!-- table: widths=5.6,6,6 caption="Ingress rules replaced by Gateway API resources" bold=first size=8.5 -->
| Former Ingress rule | Gateway API | Controller settings |
|---|---|---|
| bibs-users: host, prefix api to bibs-web | HTTPRoute bibs-users, PathPrefix api to bibs-web:8443 | Target group ip, HTTPS, health check readyz |
| bibs-users: prefix / to bibs-frontend | HTTPRoute bibs-users, PathPrefix / to bibs-frontend:8443 | Target group ip, HTTPS, health check healthz |
| bibs-integration (evaluated first), prefix integration | HTTPRoute bibs-integration; longest prefix wins | Target group ip, HTTPS, health check readyz |
| Annotations: internal scheme, certificate, TLS policy, WAF, inbound ranges | Gateway listener https | Load balancer configuration of the gateway class: scheme, listener certificate and policy, WAF web ACL, source ranges |

# Scaling and availability

<!-- table: widths=3.4,14.2 caption="Scaling and availability rules" bold=first size=8.5 -->
| Mechanism | Rule |
|---|---|
| Horizontal Pod Autoscaler | bibs-web: minimum 3, maximum 10 in PROD (UAT 2 to 4); target CPU 65 %, memory 80 %; scale up at most 2 pods a minute after 60 s; scale down 1 pod every 2 minutes after 5 minutes. bibs-frontend: 2 to 4 at CPU 70 % |
| Fixed replicas | bibs-jobs 2 pods across zones; the Valkey job lock runs each job once per schedule (one pod records SUCCEEDED, the other SKIPPED_LOCKED). bibs-integration 2 pods; the relay runs under the same lock and consumers share their groups |
| Batch windows | Month-end and the nightly windows (20:00 booking batch and remittance extraction, 22:15-23:00 collection runs, 05:00 application file) are sized by the bibs-jobs requests; more jobs pods do not speed up one job, so a heavy window is given more CPU and memory per pod by a scheduled overlay change if the performance test shows the need |
| Spread and disruption | Topology spread across zones (maximum skew 1) and hosts; disruption budgets keep at least one pod of each served workload during node drains and cluster upgrades |
| Rolling update | Maximum unavailable 0, maximum surge 1; the new pod must be ready before an old one stops |
| Node scaling | Cluster autoscaler (or Karpenter) adds nodes when pods are pending; node groups span three zones |
| Database connections | Pool of 30 connections per pod in PROD (10 in UAT); at 10 web, 2 jobs and 2 integration pods the database sees at most 420 connections, within the limit of the PROD instance class |

# Configuration and secrets

## Where settings come from

<!-- table: widths=3.6,6.4,7.6 caption="Configuration sources" bold=first size=8.5 -->
| Source | Holds | Changed by |
|---|---|---|
| Overlay of the environment | Image digests, profile (prod, or seed in SIT and UAT), environment name, allowed origin, pool size, sizing, gateway component, host name, certificate and WAF identifiers, network ranges, IAM roles | Release team through the pipeline, after review |
| ConfigMap bibs-backend-config | Non-secret settings: TLS modes, Valkey and Kafka addresses, storage buckets and KMS key identifiers, management port, business zone, job schedules (UTC cron) | Release team |
| Secret bibs-backend-secrets | Database logins of the application and of the schema owner, access-token signing key, second-factor encryption key, masking key, Valkey token, Kafka SASL credentials, SMTP credentials, OIDC client secret, initial administrator password (first start only) | Created from AWS Secrets Manager by the External Secrets Operator (DA-02); never in a manifest or an image |
| System parameters in the database | Business rules and security parameters (Solution Architecture, chapter 9) | BDOI administrators on screen, maker-checker |

## Secrets handling

- Every secret lives in AWS Secrets Manager in the account of the environment, encrypted with a BDOI-owned KMS key; the External Secrets Operator writes the Kubernetes secret, and rotation in Secrets Manager reaches the pods at their next restart.
- The backend refuses to start outside a developer machine when a secret is missing, too short (the signing key needs at least 32 characters), a development value or, in production, when any connection could run in plaintext, when the seed profile is active, or when the schema owner and the application login are the same.
- Keys rotate without sign-out loss where possible: the second-factor key keeps a previous key during rotation; rotating the token signing key signs everyone out once.
- AWS access uses IRSA only: no AWS keys in variables or secrets.

## Values BDO IT provides per environment

<!-- table: widths=9.6,8 caption="Values provided by BDO IT" bold=first size=8.5 -->
| Value | Owner |
|---|---|
| Host name on the BDO domain and its certificate | BDO PKI and network team |
| AWS WAF web ACL (managed rule groups, rate rule, Apigee-only rule) | BDO security team |
| Client network range and the Apigee X source range of the private route | BDO network team |
| Subnet ranges of the load balancer, data services and PrivateLink interfaces | BDO cloud platform team |
| AWS Private CA for in-cluster certificates; IAM roles of the three backend service accounts | BDO cloud platform and security teams |
| Apigee X key set address, token issuer and audience; scopes per published API | BDO API team |
| ECR registry; secrets in AWS Secrets Manager | Release team |
| Database logins: schema owner and least-privilege application login | DBA |

# Release pipeline and promotion

## Pipeline

![Release pipeline and promotion](figures/da_pipeline.dot){width=14.5}

<!-- table: widths=3,9,5.6 caption="Pipeline stages (every change)" bold=first size=8.5 -->
| Stage | Checks | Fails on |
|---|---|---|
| Compile and verify (backend) | Formatting, compile with all warnings as errors, test suites on a real PostgreSQL, coverage, Checkstyle, PMD and copy-paste detection, SpotBugs with its security rule set, architecture rules, CycloneDX SBOM | Any finding; coverage under 80 % of lines or 65 % of branches |
| Compile and verify (web client) | Formatting, lint with the SonarJS, accessibility and React rules, strict type check, tests with coverage, packaging, SBOM | Any finding |
| Quality | SonarQube analysis of backend and web client, quality gate | Quality gate failed |
| Security | CodeQL and SAST for Java and TypeScript; OWASP dependency-check; npm audit; gitleaks over the whole history | CVSS 7.0 or higher; high or critical advisory; any secret, unless accepted under ADR-14 |
| Images | bibs-backend and bibs-frontend on pinned base digests | - |
| Image scan | Trivy on both images (OS packages, application archive, secrets) | High or critical finding with a fix available, unless accepted under ADR-14 |
| Client documents | Pack check of the client documents | Duplicates, older versions, restricted wording |

## Promotion through the environments

<!-- table: widths=2.2,4.6,5.4,5.4 caption="Promotion gates" bold=first size=8.5 -->
| Environment | Deployed | Entry gate | Exit gate |
|---|---|---|---|
| DEV | Every change that passed the pipeline | Pipeline green | - |
| SIT | A release candidate (release tag) | Pipeline green; release note with the migrations and configuration changes | SIT exit criteria of the drop's test plan; no open severity 1 or 2 finding |
| UAT | The same digest as SIT | SIT exit report; UAT readiness statement (Drop 1 by 30-Jul-2027, Drop 2 by 30-Sep-2027) | BDOI UAT sign-off per drop |
| Pre-Prod | The same digest as UAT | UAT sign-off | Performance test at 429 concurrent sessions; penetration test findings closed or accepted; cut-over dress rehearsal |
| PROD | The same digest as Pre-Prod | Change approval (CAB), ORR / PRR evidence, go / no-go | Post-deployment checks: health of every workload, smoke test by BDOI key users, no error-rate increase in the first hour |
| DR | The same digest as PROD, right after PROD | PROD deployed | DR overlay current; standby pods healthy |

## Release rules

- **Versioning.** Each release carries a semantic version and a release tag; images are pushed to ECR with immutable tags and promoted by digest. The release record holds the digests, the SBOMs, the scan results, the list of schema migrations and the configuration changes.
- **Schema changes.** Migrations run at start-up of the first pod (others wait on the migration lock), as the schema owner login. They are backward compatible with the previous release (expand, migrate, contract over two releases), so the rolling update can run old and new pods together. A manual RDS snapshot is taken before every PROD release.
- **Window.** Production releases run in the maintenance window 00:00-04:00 (SA-11), outside the night jobs; urgent fixes follow the same pipeline with an emergency CAB.
- **Rollback.** Redeploy the previous digest with the previous overlay. A migration is never rolled back by hand: a fix is a new migration. If a release must be withdrawn with its schema change, the RDS snapshot of the release is the fallback, decided by the go / no-go owner.
- **Deployment tool.** The pipeline applies the overlay with kubectl through the bastion (SSM) or hands it to the GitOps tool BDO standardises on (Argo CD in the IER diagram); the manifests are the same (DA-01, IQ29).

# Responsibilities

<!-- table: widths=5.6,2.4,2.4,2.4,2.4,2.4 caption="Deployment responsibilities (R responsible, A accountable, C consulted, I informed)" bold=first size=8 -->
| Activity | BDO IT Cloud and Digital Operations | BDO network and security | BDOI IT Operations | iorta TechNXT DevOps | iorta TechNXT Tech Lead |
|---|---|---|---|---|---|
| EKS clusters, node groups, add-ons | A, R | C | I | C | I |
| Gateway controller, WAF, certificates, source ranges | R | A | I | C | I |
| Manifests (base, components, overlays) | C | C | I | R | A |
| Secrets in AWS Secrets Manager | A | C | R | C | I |
| Pipeline definition on the BDO toolchain | A | C | I | R | C |
| Deployment to SIT and UAT | I | I | I | R | A |
| Deployment to Pre-Prod, PROD and DR | R | I | A | C | C |
| Rollback decision in PROD | C | I | A | C | R |

# Decisions and open points for BDOI

<!-- table: widths=1.2,3,8.4,2.8,2.2 caption="Decisions and open points for BDOI" bold=first size=8 -->
| ID | Topic | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| DA-01 | CI/CD toolchain and deployment tool | Run the pipeline on the BDO toolchain named in the IER; apply overlays from the pipeline through the SSM bastion; adopt Argo CD later if BDO standardises on GitOps (IQ29) | BDO IT Cloud and Digital Operations | 30-Nov-2026 |
| DA-02 | Secrets delivery | External Secrets Operator reading AWS Secrets Manager in each environment | BDO IT Cloud and security | 30-Nov-2026 |
| DA-03 | Certificates | Browser-facing certificate under the BDO corporate PKI imported in ACM; in-cluster certificates from AWS Private CA through cert-manager | BDO PKI team | 18-Dec-2026 |
| DA-04 | Gateway controller | AWS Load Balancer Controller (internal ALB with WAF) in every environment; Envoy Gateway only with the security team's agreement | BDO network and security | 30-Nov-2026 |
| DA-05 | Node sizes | 3 to 6 m5.2xlarge nodes in PROD and Pre-Prod instead of 3 m5.4xlarge; confirmed by the performance test | BDO IT Cloud and Digital Operations | 30-Nov-2026 |
| DA-06 | Container base images | Keep Eclipse Temurin and nginx unprivileged on Alpine, pinned by digest; switch to RHEL UBI 9 only if BDO requires it (IQ29) | BDOI Enterprise Architecture | 30-Nov-2026 |
| DA-07 | Non-production hours | 12 x 5 for the EKS nodes and RDS of DEV, SIT, UAT and Pre-Prod, with extended hours booked for batch and month-end cycles, trial migrations and the dress rehearsal; job schedules moved into the window by configuration (IQ27) | BDOI IT | 30-Nov-2026 |
| DA-08 | Release window and cadence | Production releases in the 00:00-04:00 window; after go-live a monthly release and emergency fixes through the same pipeline | BDOI IT Operations | 30-Sep-2027 |

# Glossary {-}

```glossary
ACM: AWS Certificate Manager
CAB: Change Advisory Board
CRD: Custom resource definition (Kubernetes)
ECR: Amazon Elastic Container Registry
EKS: Amazon Elastic Kubernetes Service
HPA: Horizontal Pod Autoscaler
IRSA: IAM roles for service accounts
NLB: Network Load Balancer
ORR / PRR: Operational / production readiness review
PDB: Pod disruption budget
SBOM: Software bill of materials
SSM: AWS Systems Manager (session access without inbound ports)
WAF: Web application firewall
```
