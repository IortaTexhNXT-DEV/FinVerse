# Claims

Package `com.iortatechnxt.finverse.claims`, UI section **Claims** (`frontend/src/features/claims`),
migrations `V200__claims.sql` (schema, event type, exception codes) and `V920__demo_claims.sql`
(demo roles, parties and accounting rule), demo loader `claims.demo.ClaimsDemoData`.

Claims depends on underwriting (read API `PolicyQueryService`, snapshots only), party, sub-ledger,
accounting, the insurance shared kernel and the platform modules. **No module depends on claims**:
underwriting, approval and the kernel see it only through the ports it implements.

## 1. Concepts

- **The claim is at 100 %.** Every amount is entered at 100 % of the loss. The company share (the
  policy's share %) is derived on each running total, so the shares of all movements always add up to
  the share of the total (no rounding drift). All accounting, the movement ledger, the kernel
  movements and the reports are at the company share.
- **Estimate (reserve)** per side and cost type: *payment* (loss or expense) and *recovery* (loss
  only). Payment outstanding = payment estimate − paid; it is the claim reserve in the ledger.
  Recovery estimates are memorandum (no journal); recoveries cannot exceed them.
- **Reports Book types** of each estimate / paid line: 1 Payment, 2 Recovery, 3 Reversal of Payment
  (payment estimate decrease, negative), 4 Reversal of Recovery (negative). Hence estimate payment =
  types 1 + 3 and estimate recovery = types 2 + 4. Paid lines are types 1 (settlement) and 2
  (recovery received).
- **Movement ledger** (`clm_movement`, `MovementLine`): one immutable line per approved estimate
  change, settlement or recovery, with the amount at 100 %, company share and base currency (SPOT rate
  of the movement date), a unique reference and the journal batch. Reports, the kernel views and the
  listener notifications are all derived from it.
- **Currency.** The claim currency is the policy currency (rule `CLAIM_CURRENCY_MISMATCH`).

## 2. Lifecycle

| Status | Reached by | Next |
|---|---|---|
| `REGISTERED` | notification (FNOL) | OPEN, REJECTED, WITHDRAWN |
| `OPEN` | first payment estimate approved | PARTIALLY_SETTLED, CLOSED, REJECTED, WITHDRAWN |
| `PARTIALLY_SETTLED` | a settlement approved | CLOSED |
| `CLOSED` | final settlement approved, or *close* | REOPENED |
| `REOPENED` | *reopen* | PARTIALLY_SETTLED, CLOSED, REJECTED, WITHDRAWN (if nothing paid) |
| `REJECTED` | *repudiate* (reason) | – |
| `WITHDRAWN` | *withdraw* (reason) | – |

- **Notification**: the policy must be approved and in force at the loss date
  (`PolicySnapshot.isInForce`), the notification date cannot be before the loss or in the future.
  Number `CL-<branch>-<year of notification>-nnnnnn`. Policy facts (product, class, client,
  intermediary, UW year, share, coinsurance, risk and sum insured) are copied onto the claim. The
  claimant defaults to the policyholder; surveyors, adjusters, third parties and garages are parties
  of the party master (`ClaimPartyRole` lists the allowed party types). Optional initial loss and
  expense reserves are submitted for approval with the registration.
- **Close, repudiate, withdraw** release any outstanding reserve through *system* reserve changes
  (approved as part of the decision). They are checker decisions (`CLAIM_AUTHORIZE`), never taken by
  the user who registered the claim, and are refused while documents of the claim wait for approval.
  Repudiation and withdrawal are refused once anything was paid.
- **Final settlement** releases the remaining payment reserve (loss and expense) and closes the claim.

## 3. Maker-checker and authorization limits

Reserve changes, settlements and recoveries are documents: the maker (`CLAIM_MAINTAIN`) enters them
(`PENDING_APPROVAL`); a checker (`CLAIM_AUTHORIZE`, never the maker) approves or rejects them. The
checker's user authorization limit (the same one journals and payments use) applies to:

| Document | Amount compared with the limit (base currency) |
|---|---|
| Reserve change increasing a payment estimate | resulting total payment estimate of the claim, company share |
| Reserve decrease, recovery estimate | none (reduces the exposure) |
| Settlement | amount payable to the payee |
| Recovery | none (money received) |

Pending documents appear in **My Approvals** (`ClaimApprovalSource`), without the viewer's own
documents and without those above the viewer's limit. Only one pending change per side and cost type
is allowed, so the "previous estimate" of an approval is always well defined.

## 4. Accounting (company share)

| Event (source reference) | Components | Demo rule | When |
|---|---|---|---|
| `CLAIM_RESERVE` (`CLAIM:<id>:RSV:<change id>`) | RESERVE_CHANGE (signed delta) | Dr 5200 Change in claims reserves / Cr 2102 Outstanding claims reserve (negative = release) | payment estimate change approved, system release |
| `CLAIM_SETTLEMENT` (`CLAIM:<id>:STL:<settlement id>`) | PAID_AMOUNT, RESERVE_RELEASE (both = company share of the net) | Dr 5100 Claims paid / Cr 2204 Claims payable (payee party line); Dr 2102 / Cr 5200 | settlement approved |
| `CLAIM_RECOVERY` (`CLAIM:<id>:REC:<recovery id>`) | AMOUNT, role BANK | Dr bank / Cr 5100 | recovery approved |
| `CLAIM_COINSURANCE` (reference + `:COINS`) | COINSURER_SHARE | Dr 1206 Due from coinsurers (coinsurer party line) / Cr 2204 | settlement of a claim whose coinsurance the company leads |
| | COINSURER_RECOVERY, role BANK | Dr bank / Cr 2203 Due to coinsurers (coinsurer party line) | recovery on a led coinsurance |

`CLAIM_COINSURANCE` is created in V200; its demo rule is in V920. `CLAIM_PAYMENT` (Dr 2204 / Cr bank)
is posted by Payables when the payment voucher is approved.

**Open items (sub-ledger)**, recorded in the approval transaction:

| Party | Direction | Document type | Amount |
|---|---|---|---|
| payee (claimant, garage, surveyor) | CREDIT | `CLAIM_SETTLEMENT` | net at 100 % when leading a coinsurance, else company share |
| coinsurer (leading) | DEBIT | `COINSURER_CLAIM_SHARE` | coinsurers' share of the settlement (recoverable) |
| coinsurer (leading) | CREDIT | `COINSURER_RECOVERY_SHARE` | coinsurers' share of a recovery (payable) |

The settlement item is due at once. Its document type contains "CLAIM", so the Payables payment
voucher defaults to the **CLAIM** payment category (event `CLAIM_PAYMENT`) and settles it.
When the company follows a coinsurance, it settles only its own share; the leader pays the claimant.

## 5. Insurance kernel integration

- **Listeners**: after every posted movement, in the same transaction, every
  `insurance.ClaimMovementListener` bean is called (`ClaimMovementNotifier`; none deployed = no-op).
  Translation (`KernelMovements`):
  - payment estimate change → `RESERVE_CHANGE` ± delta (line reference);
  - settlement → `PAYMENT` + amount (line reference) and `RESERVE_CHANGE` − amount (reference +
    `:RSV`), since a payment consumes the reserve;
  - recovery received → `RECOVERY` + amount;
  - recovery estimate change → nothing (memorandum).

  So the running sum of `RESERVE_CHANGE` movements is the outstanding reserve. References are stable
  and unique; a document can be approved only once, so each reference is delivered exactly once.
- **`ClaimsExperienceView`** (`ClaimsExperienceService`): `outstanding(company, asOf)` = payment
  estimate − paid (loss and expense) of the lines dated up to `asOf`, claims with a non-zero amount,
  in claim and base currency (base = historical rates of the lines); `movements(company, from, to)` =
  the kernel movements of the lines dated in the range, in date order.
- **`underwriting.service.PolicyClaimsView`** (`PolicyClaimsService`): per policy, claim count
  (withdrawn claims excluded), latest claim (latest loss date), reserve and outstanding of claims still
  handled, paid net of recoveries, net claims = paid + outstanding; company share, policy currency.
  Underwriting uses it in PGIBR013 and PGIBR084.

## 6. LPO (local purchase orders)

Motor claims only (`LPO_MOTOR_ONLY`). An LPO orders a garage repair, own damage (OD) or third party
(TP): gross, discount, net = gross − discount; number `LPO-<branch>-<year>-nnnnnn`; cancellable with a
reason. The garage is added to the claim's parties. An LPO is a commitment, not an accounting entry:
the garage is paid through a settlement to the garage.

## 7. Alerts (exception codes seeded in V200)

| Code | Severity | Threshold | Raised when |
|---|---|---|---|
| `LARGE_CLAIM_RESERVE` | HIGH | 5,000,000.00 | an approved payment reserve makes the company-share estimate (base currency) reach the threshold |
| `LATE_CLAIM_NOTIFICATION` | MEDIUM | 30 days | a claim is reported more than the threshold days after the loss |

One live alert per claim and code (dedup key `<code>:<claim id>`).

## 8. Reports (category Claims, permission CLAIM_VIEW)

Range parameters (branch, class, product, customer, broker) are optional; amounts are company share
in base currency unless stated.

| Code | Title | Rules |
|---|---|---|
| PGIBR002 | Claims Settled Statement | claims with amounts paid or recovered between the paid dates; estimates (types 1+3 / 2+4) as at the paid-to date; totals payment − recovery; Closed Y/N; include-expense option; Branch > Class > Product |
| PGIBR018 | Claims Outstanding | as on a date: payment O/S = estimate − paid, recovery O/S, net O/S = payment O/S − recovery O/S; claims closed / declined by then or reported after it excluded; include-expense option; Branch > Class > Product > UW Year |
| PGIBR036 | Client-wise Outstanding Claims | open claims with payment O/S as on a date; Branch > Customer |
| PGIBR012 | Claims Ratio by Class of Business | approved policies expiring in the period; premium of all approved transactions; paid (net of recoveries) and O/S as at the expiry-to date; ratio = (paid + O/S) / our net premium × 100 (0 without premium); New / Renewed; Branch > Class > Product |
| PGIBR028 | Claims Ratio – Policy | policies with premium approved in the period; net claim = paid + closing O/S − opening O/S; release = opening − paid − closing; Branch > Class > Product > Customer |
| PGIBR023 | Production / Claims Analysis | our gross premium approved in the period by kind (New, Renewal, Additional = positive endorsement, Refund = negative endorsement), total gross 100 % and our share; claims with a loss date in the period as at its end (FinVerse rule); Branch > Class |
| PGIBR082 | LPO Issued (OD and TP) | issued LPOs of claims with a loss date in the period, per claim and garage: count, gross, discount, net; cover filter; claim currency; Branch > Garage > Product |
| CLM-REGISTER | Claims Register (Bordereaux) | claims reported in the period with loss details, status and figures as at the period end; Branch > Class |
| CLM-MOVEMENT | Claim Movement Statement | every movement of one claim, company share in the claim currency, with the running payment O/S |

## 9. API (`/api/v1/claims`)

| Method | Path | Permission |
|---|---|---|
| GET | `?companyId=&status=&businessLine=&q=&lossFrom=&lossTo=&page=&size=` | CLAIM_VIEW |
| GET | `/{id}`, `/{id}/movements`, `/{id}/reserves`, `/{id}/settlements`, `/{id}/recoveries`, `/{id}/lpos`, `/lpos?companyId=`, `/policy-cover?companyId=&policyNo=&lossDate=` | CLAIM_VIEW |
| POST | `` (register), `/{id}/parties`, `/{id}/reserves`, `/{id}/settlements`, `/{id}/recoveries`, `/{id}/lpos`, `/lpos/{lpoId}/cancel` | CLAIM_MAINTAIN |
| POST | `/reserves/{id}/approve|reject`, `/settlements/{id}/approve|reject`, `/recoveries/{id}/approve|reject`, `/{id}/close`, `/{id}/reopen`, `/{id}/repudiate`, `/{id}/withdraw` | CLAIM_AUTHORIZE |

Approvals take an optional `{ "accountingDate": "yyyy-MM-dd" }` (default today, never before the
notification date).

## 10. Demo data

`V920` grants `CLAIM_AUTHORIZE` to the finance manager (`fmanager`, unlimited) and the authorizer
(`checker`, limit 5,000,000), and adds surveyors (`SV-0001`, `SV-0002`), third-party claimants
(`TP-0001`, `TP-0002`), a salvage buyer (`S-0010`) and the `CLAIM_COINSURANCE` rule, all for FVI.

`ClaimsDemoData` (`@Profile("demo")`, `@Order(20)`, after underwriting at 10 and before payables at
40; skipped when the company has claims) plans about 60 claims (`DemoClaimPlan`, deterministic) with
losses from February to July 2026 over the demo policies and runs them through the services
(`DemoClaimScenarios`, maker `claims`, checker `fmanager`): open, partially settled, final settlement
with deductible and surveyor fee, reserve increase and decrease, repudiated, withdrawn, reopened with a
new reserve, pending reserve increase and pending settlement (for My Approvals), salvage with recovery
estimate and receipt, a USD marine claim, coinsured claims (leading and following), LPOs for every
motor claim with the garage as payee, and late notifications (`LATE_CLAIM_NOTIFICATION`). Settlements
stay unpaid: Payables shows them as claim payables ready for payment vouchers.
