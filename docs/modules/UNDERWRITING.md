# Underwriting (products, quotations, policies, endorsements, open covers)

Package `com.iortatechnxt.finverse.underwriting`, UI section **Underwriting**
(`frontend/src/features/underwriting`), migration `V100__underwriting.sql` (schema), accounting event
types in `V3__party_subledger_accounting_engine.sql`, demo `V901__demo_parties_and_accounting_rules.sql`
(parties and posting rules) and `V910__demo_underwriting_roles.sql` (approver grant), demo loader
`underwriting.demo.UnderwritingDemoData` (`@Profile("demo")`, `@Order(10)`, idempotent).

Underwriting depends on party, sub-ledger, the accounting engine, currency, organization, dimension,
audit and the approval inbox. **It depends on neither claims nor reinsurance**: it publishes a read
API (`PolicyQueryService`) and owns two ports (`PolicyReinsuranceView`, `PolicyClaimsView`) that
those modules implement (section 9).

Every amount of a policy is stored per **financial document**: the policy row holds the original
issue, each endorsement row holds the change, with the same column set (`PremiumBreakdown`), so the
registers can union them. Premium is entered at 100 % of the risk; the company share, taxes and
commission are derived.

## 1. Products and their rates

A product is a class of business of one company, under maker-checker control (`AuthorizableEntity`):
a new or changed product is `PENDING_AUTHORIZATION` until a different user with `POLICY_AUTHORIZE`
authorizes it, and only an authorized product can be used on a new quotation, policy, conversion,
open cover or certificate (`ProductService.requireActive`).

| Attribute | Meaning |
|---|---|
| `code`, `name` | unique per company; the code is part of the policy number |
| `businessLine` | line of business (`BUSINESS_LINE` dimension); carried on every journal line and used by reinsurance treaties, reserves and the IC schedules |
| `defaultCommissionRate` | commission % used when neither the policy nor the intermediary gives one |
| `uprBasis` | earning basis for the unearned premium reserve: `DAYS_365` (1/365 daily), `TWENTY_FOURTHS` (1/24 monthly), `EIGHTHS` (1/8 quarterly); read by actuarial reserves |
| `dstRate` | documentary stamp tax % |
| `vatRate` | value added tax % |
| `lgtRate` | local government (premium) tax % |
| `fstRate` | fire service tax % (fire business only) |
| `premiumTaxRate` | percentage (premium) tax %, for business not subject to VAT |
| `policyFee` | flat fee charged on new policies and renewals |
| `openCoverAllowed` | whether marine open covers and certificates may be written |

All tax rates are **percent of the company's net premium** (never of the policy fee or of the
coinsurers' share). The rates are configurable per product; typical Philippine non-life values
(`TaxRates`) are DST 12.5, VAT 12, LGT 0.75 (depends on the LGU), FST 2 on fire business and premium
tax 2 for business not subject to VAT. The demo products (section 12) follow these values.

## 2. Premium calculation (`PremiumBreakdown`)

`PremiumBreakdown.calculate(PremiumInput)` is the single premium formula for policies, endorsements,
quotation conversion and certificates. `PolicyPremiumCalculator` supplies its inputs: gross premium
and sum insured = sum of the risks (a risk's premium is entered, else sum insured × rate %), taxes and
policy fee from the product, commission and withholding from the policy and the intermediary.

1. **Gross, discount, loading (100 %)**: discount = gross × discount %, loading = gross × loading %,
   net = gross + loading − discount.
2. **Our share** of every amount = amount × share % (100 when not coinsured).
3. **Billed premium** = our net, plus the coinsurers' share (net − our net) when the company leads
   the coinsurance and collects 100 % from the client.
4. **Taxes** on our net premium: DST, VAT, LGT, FST and premium tax at the product rates; plus the
   **policy fee**.
5. **Total due** from the client = billed premium + taxes + policy fee.
6. **Commission** = our net × commission %; **withholding tax** = commission × the intermediary's
   withholding rate; **net commission** = commission − withholding tax.

Every amount is rounded to 2 decimals (half-even); signs follow the gross premium (negative for return
premium). The commission rate is the rate entered on the policy (0–100 %), else the intermediary's
rate, else the product default; direct business has no commission. Preview, save and update use the
same rule, so a saved draft carries exactly the commission its preview showed.

### Worked example (fire, broker business)

Product FIRE-COM (DST 12.5 %, VAT 12 %, LGT 0.75 %, FST 2 %, premium tax 0, policy fee 250.00); sum
insured 10,000,000.00; gross premium 100,000.00; discount 10 %; loading 5 %; broker with commission
20 % and withholding 10 %. The 100 % column reproduces `PremiumBreakdownTest`.

| Line | 100 %, direct | 60 % share, leader | 60 % share, follower |
|---|---:|---:|---:|
| Sum insured / our sum insured | 10,000,000.00 / 10,000,000.00 | 10,000,000.00 / 6,000,000.00 | same |
| Gross premium | 100,000.00 | 100,000.00 | 100,000.00 |
| Discount (10 %) | 10,000.00 | 10,000.00 | 10,000.00 |
| Loading (5 %) | 5,000.00 | 5,000.00 | 5,000.00 |
| Net premium (100 %) | 95,000.00 | 95,000.00 | 95,000.00 |
| Our net premium (60,000 + 3,000 − 6,000 when coinsured) | 95,000.00 | 57,000.00 | 57,000.00 |
| Coinsurers' share billed | – | 38,000.00 | – |
| **Billed premium** | 95,000.00 | 95,000.00 | 57,000.00 |
| DST 12.5 % of our net | 11,875.00 | 7,125.00 | 7,125.00 |
| VAT 12 % | 11,400.00 | 6,840.00 | 6,840.00 |
| LGT 0.75 % | 712.50 | 427.50 | 427.50 |
| FST 2 % | 1,900.00 | 1,140.00 | 1,140.00 |
| Policy fee | 250.00 | 250.00 | 250.00 |
| **Total due (debit note)** | **121,137.50** | **110,782.50** | **72,782.50** |
| Commission 20 % of our net | 19,000.00 | 11,400.00 | 11,400.00 |
| Withholding tax 10 % | 1,900.00 | 1,140.00 | 1,140.00 |
| Net commission payable | 17,100.00 | 10,260.00 | 10,260.00 |

## 3. Quotations

Lifecycle (`Quotation`, `QuotationStatus`):

```
DRAFT --submit--> PENDING_APPROVAL --approve--> APPROVED --convert--> CONVERTED
                                   --reject---> REJECTED
DRAFT / APPROVED / REJECTED --new iteration--> DRAFT
DRAFT / PENDING_APPROVAL / APPROVED --validity lapsed (expire run)--> EXPIRED
```

- Number `Q-<branch>-<year>-nnnnnn`. Terms: product, client, insured, channel and intermediary,
  issue date, validity days (1–365), cover period, currency, share % and commission % (default: the
  product's; zero for direct business).
- **Iterations** record the negotiation: sum insured, gross premium, discount, loading and charges
  (amounts, not rates) with remarks. A new iteration re-opens the quotation as `DRAFT`; the last
  iteration is the one offered. The discount cannot exceed the gross premium.
- **Maker-checker**: the user who created or submitted the quotation cannot approve or reject it.
- **Validity**: expiry date = issue date + validity days. `POST /quotations/expire?companyId&asOf`
  (`POLICY_AUTHORIZE`) marks open quotations past their expiry as `EXPIRED`.
- **Conversion** (`POLICY_MAINTAIN`) of an approved quotation that is still valid on the policy issue
  date creates a **draft policy** with one risk carrying the quoted sum insured and gross premium
  (rate = premium / sum insured); discount and loading become rates on the policy; a share below
  100 % becomes coinsured business, for which the conversion names the coinsurer and whether the
  company leads. The quotation's commission % is carried to the policy and the quotation records the
  policy id. The policy then follows the normal policy lifecycle; its taxes and policy fee are
  computed from the product (the quoted "charges" are not carried over).

## 4. Policies and maker-checker

Lifecycle, shared by policies, marine certificates and endorsements (`ApprovalWorkflow`,
`PolicyStatus`):

```
DRAFT --submit--> PENDING_APPROVAL --approve--> APPROVED --cancellation endorsement--> CANCELLED
                                   --reject (reason)--> DRAFT
DRAFT --discard--> CANCELLED
```

- Number `P-<product>-<branch>-<year of issue>-nnnnnn`; certificates `MC-<branch>-<year>-nnnnnn`.
  All underwriting series are gapless per branch and year (`DocumentNumberService`).
- **Validation** (`UnderwritingRules`): period to ≥ period from; the customer is a client party;
  direct business has no intermediary, agent business an `AGENT`, broker business a `BROKER`; direct
  business is written at 100 % without coinsurer, coinsured business needs a `COINSURER` party and a
  share above 0 and below 100 %; at least one risk. The underwriting year is the year the cover
  starts.
- **Risks**: description, sum insured, rate, premium, occupation and accumulation zone (used by the
  accumulation report), and for marine the vessel, voyage, sailing date, B/L, L/C, bank and basis of
  valuation.
- **Maker** (`POLICY_MAINTAIN`): preview, create, update and submit drafts, discard a draft. Only a
  draft can be edited; every save recomputes the premium.
- **Checker** (`POLICY_AUTHORIZE`): approves with an optional accounting date (default today) or
  rejects back to draft with a reason. The checker must differ from both the **creator and the
  submitter** (`MAKER_CHECKER_VIOLATION`). **Underwriting applies no authorization limit.**
- **Approval is one transaction** (`PolicyApprovalService`): status change, accounting events,
  debit / credit notes and open items either all succeed or nothing changes. If no authorized rule
  exists for an event, the approval fails and the failure is visible in the Event Register.
- **In force**: a policy covers a date when it is approved (or was cancelled after that date) and the
  date is within its period (`isInForce`, used by claims at notification).
- Pending policies, endorsements and quotations, and products and open covers pending authorization,
  appear in **My Approvals** (`UnderwritingApprovalSource`), never to their creator or submitter.
- Every create, update, submit, approve, reject and discard is written to the audit trail.

## 5. Endorsements

An endorsement changes an **approved** policy. It carries its own premium figures (the change),
workflow and debit or credit note; its number is `<policy no>/Enn` (E01, E02…). Only **one
endorsement per policy may be open** (draft or pending) at a time, so pro-rata figures are always
computed on the approved position. The effective date must fall within the policy period; for a
renewal it may be later, and the new period must start after the current one.

| Type | Premium (on the policy's share, discount and loading rates, product taxes and the original commission rate) | Event on approval | Effect on the policy |
|---|---|---|---|
| `ADDITIONAL` | entered gross premium and sum insured change (positive) | `POLICY_ENDORSEMENT` | none |
| `REFUND` | entered gross premium and sum insured change, negated | `POLICY_ENDORSEMENT` (negative) | none |
| `RENEWAL` | renewal gross premium (default: the original gross) and sum insured (default: the original), **plus the policy fee** | `POLICY_ISSUE` | period replaced by the new period, which must start after the current one |
| `CANCELLATION` | pro-rata return, 1/365: − gross of the current period × unexpired days / period days (unexpired days counted from the effective date, inclusive); sum insured returned in full; no policy fee | `POLICY_CANCELLATION` (negative) | status `CANCELLED`, `cancelledOn` = effective date |
| `NIL` | none (address, name, description) | none | none |

The *current period* position is the original issue, or the latest approved renewal, plus the
approved endorsements made after it. A financial endorsement whose total due and commission are both
zero raises no event.

**Cancellation example.** A 100,000.00 gross policy of the worked example (1 January to
31 December 2026, 365 days) cancelled with effect from 1 July 2026: 184 unexpired days, ratio
0.5041095890, return gross −50,410.96, discount +5,041.10, loading −2,520.55, net −47,890.41; DST
−5,986.30, VAT −5,746.85, LGT −359.18, FST −957.81; total due **−60,940.55** (a credit note to the
client); commission −9,578.08, withholding −957.81, net commission −8,620.27 (a debit note to the
broker).

## 6. Coinsurance (leader / follower)

`business_type` is `DIRECT` (100 %) or `DIRECT_WITH_COINSURANCE` with the company's share %, one
coinsurer party and the leader flag.

- **Leader** (`coinsuranceLeader = true`): the company bills the client 100 % of the net premium
  (billed premium) plus the taxes on its own share, and owes the coinsurers their share
  (`COINSURANCE_SHARE`, credit open item on the coinsurer). On claims, the leader settles 100 % and
  recovers the coinsurers' share (see [CLAIMS.md](CLAIMS.md)).
- **Follower**: the company bills and accounts only its share; the leader collects the rest.
- Taxes, commission and reinsurance always follow the company's share (our net premium).

## 7. Open covers and marine certificates

An open cover (`OC-<branch>-<year of period from>-nnnnnn`) is a master marine cargo policy of a
client: product (must allow open covers), period, currency, **limit per shipment**, **annual
limit**, rate % and cargo description. It is maker-checker master data: it accepts declarations only
once authorized by another user (`POLICY_AUTHORIZE`).

Declaring a shipment (`POST /open-covers/{id}/certificates`, `POLICY_MAINTAIN`) creates a **marine
certificate**: a draft policy numbered `MC-…`, linked to the cover, written at 100 % (no
coinsurance) without discount or loading, with the shipment as its only risk. The channel is direct
unless an agent or broker is named on the declaration.

- The sailing date (default: the issue date) must fall within the cover period; the certificate
  period runs from the sailing date for the transit days (default 60).
- The sum insured must not exceed the limit per shipment, and the sum insured of the cover's draft,
  pending and approved certificates must stay within the annual limit.
- The premium defaults to sum insured × cover rate % unless a premium or rate is entered.
- The certificate is then submitted and approved like any policy; its approval posts `POLICY_ISSUE`
  (including the product's policy fee) and issues the debit note.

## 8. Accounting

All postings go through the accounting engine (`AccountingEventPublisher`); the module never picks
GL accounts. Events are published by `PremiumPostingService` inside the approval transaction, in the
policy currency, on the accounting (approval) date, with the product's line of business. The
exchange rate is the **SPOT** rate of the accounting date and is stored on the document
(`PostingRefs.exchangeRate`). A negative amount posts to the opposite side of the rule line.

| Event (source reference) | When | Components | Demo rule (V901, company FVI) |
|---|---|---|---|
| `POLICY_ISSUE` (`POLICY:<id>`; renewal `POLICY:<id>:ENDT:<n>`) | policy or certificate approved; renewal approved | `GROSS_PREMIUM` (= billed premium), `DST`, `VAT`, `LGT`, `FST`, `PREMIUM_TAX`, `POLICY_FEE`, `TOTAL_DUE` | Dr 1201 Premiums receivable (`TOTAL_DUE`, client party line) / Cr 4100 Gross premiums written, Cr 2503 DST payable, Cr 2504 Output VAT payable, Cr 2505 LGT payable, Cr 2506 FST payable, Cr 2507 Premium tax payable, Cr 4700 Other income (policy fee) |
| `POLICY_ENDORSEMENT` (`POLICY:<id>:ENDT:<n>`) | additional or refund approved | same | same lines; a refund reverses them (Dr 4100 and the tax accounts / Cr 1201) |
| `POLICY_CANCELLATION` (`POLICY:<id>:ENDT:<n>`) | cancellation approved | same, negative | same lines, reversed |
| `COMMISSION_ACCRUAL` (reference + `:COMM`) | intermediated document with commission ≠ 0 | `COMMISSION`, `WITHHOLDING_TAX`, `NET_COMMISSION` | Dr 5400 Commission expense / Cr 2300 Commissions payable (`NET_COMMISSION`, intermediary party line), Cr 2508 Expanded withholding tax payable |
| `COINSURANCE_SHARE` (reference + `:COINS`) | leader, coinsurers' share ≠ 0 | `COINSURER_PREMIUM` | Dr 4100 Gross premiums written / Cr 2203 Due to coinsurers (coinsurer party line) |

For the leader, 4100 is credited with the billed premium (100 %) and debited with the coinsurers'
share, so gross premiums written equal the company's net premium. Journals of the worked example
(direct, 100 %):

```
POLICY_ISSUE        Dr 1201  121,137.50   Cr 4100  95,000.00   Cr 2503  11,875.00   Cr 2504  11,400.00
                                          Cr 2505     712.50   Cr 2506   1,900.00   Cr 4700     250.00
COMMISSION_ACCRUAL  Dr 5400   19,000.00   Cr 2300  17,100.00   Cr 2508   1,900.00
```

**Open items and note numbering** (party sub-ledger, recorded in the approval transaction with the
journal batch number; base amount at the stored rate; due date = accounting date + the party's
credit days):

| Party | Positive amount | Negative amount (return premium) | Amount | Document number |
|---|---|---|---|---|
| client | DEBIT `DEBIT_NOTE` | CREDIT `CREDIT_NOTE` | total due | `DN-<branch>-<year>-nnnnnn`, or `CN-…` for a return |
| intermediary | CREDIT `COMMISSION` | DEBIT `COMMISSION_RECOVERY` | net commission | `CN-…` for commission payable, or `DN-…` for a recovery |
| coinsurer (leader only) | CREDIT `COINSURANCE_SHARE` | DEBIT `COINSURANCE_RETURN` | coinsurers' share | the client's note number |

Debit notes and credit notes are two gapless series per branch and year of the accounting date,
shared by clients and intermediaries. The client note is stored as `debitNoteNo`, the intermediary
note as `creditNoteNo`, together with the premium and commission journal batches. Downstream:
receipts apply collections to the debit notes (`PREMIUM_RECEIPT`, Dr bank / Cr 1201), payment
vouchers pay commissions (`COMMISSION_PAYMENT`, Dr 2300 / Cr bank) and premium refunds
(`PREMIUM_REFUND_PAYMENT`, Dr 1201 / Cr bank).

## 9. Ports to claims, reinsurance and the other modules

| Interface | Direction | Implementation / consumers |
|---|---|---|
| `underwriting.service.PolicyQueryService` | public read API of underwriting (immutable snapshots, never entities) | claims (policy look-up, cover check `isInForce`, risks, claims-ratio and production reports), reinsurance (approved transactions to cede, risks, claim reports), actuarial reserves (UPR from `approvedTransactions` and `coverPeriods`, OSLR, takaful), tax (premium taxes, VAT, EWT on commissions) |
| `underwriting.service.PolicyReinsuranceView` | port owned by underwriting, implemented by reinsurance | `reinsurance.service.CessionFiguresView`: treaty (quota share + surplus), FAC and net retention premium per transaction, in the policy currency; used by PGIBR015, PGIBR016 and PGIBR013 |
| `underwriting.service.PolicyClaimsView` | port owned by underwriting, implemented by claims | `claims.service.PolicyClaimsService`: claim count, latest claim, reserve, paid, outstanding and net claims per policy (company share, policy currency); used by PGIBR013 and PGIBR084 |
| `approval.service.PendingApprovalSource` | platform port implemented by underwriting | `UnderwritingApprovalSource` |

`PolicyQueryService` methods: `findByNumber` / `get` (header of the current period), `risks`,
`isInForce`, `approvedTransactions` (approved issues and endorsements of a period with their premium
figures), `policyTransactions` (premium history of one policy), `transactions` (register selection),
`coverPeriods` (earning period of each transaction), `policiesExpiring` and `risksInForce`.
A transaction is a `PremiumTransaction` keyed by `TransactionRef` (policy id + endorsement number,
0 for the original issue).

The two ports are injected optionally (`UnderwritingPorts`, `ObjectProvider`): without an
implementation, or for a transaction without entry, reports show no treaty or FAC premium (the whole
net premium as retention, `ReinsuranceFigures.noCession`) and no claims (`ClaimsFigures.none`).
Implementations must be read-only, bulk queries. Reinsurance does not listen to approvals: its RI
Allocation screen and the `AllocationJob` read `approvedTransactions` and cede each transaction once.

## 10. Reports (category Underwriting, permission `POLICY_VIEW`)

Range parameters (branch, class, product, customer, broker / agent) are optional. "Based on" selects
the date: issue, accounting, approval or period from (accounting date = approval date in FinVerse).

| Code | Title | Rules |
|---|---|---|
| PGIBR003 | Daily Production Report | policies and endorsements per day with net premium at 100 % and our share, by issue or approval date; Branch > Class > Product |
| PGIBR005 | Premium Register with DN Number | approved policies and endorsements with the client debit note and the broker credit note, in base currency; Branch > Class > Product |
| PGIBR013 | List of Policies Due for Renewal | approved policies expiring in a period with the premium of their current period, FAC ceded and claims experience; claim ratio = net claim / our net premium × 100; Branch > Class > Product > UW Year |
| PGIBR015 | Premium Register (General) | every approved premium transaction with coinsurance split, taxes, commission and the reinsurance split (treaty = QS + surplus, net retention, FAC); Branch > Class > Product > UW Year |
| PGIBR016 | Premium Register Summary | PGIBR015 totals, one line per product; Branch > Class > UW Year |
| PGIBR025 | Broker Income Statement | brokerage earned by each broker on approved broker business; Branch > Broker |
| PGIBR027 | Schedule of Shipment under Open Policy | approved certificates of an open cover with their shipment details, in the cover currency |
| PGIBR040 | Transactions Pending Approval | policies and endorsements in draft or pending approval, in the policy currency; Branch > Class > Product |
| PGIBR042 | List of Quotations Pending Approval | at 100 % and our share; pending days 0 lists all, else those pending at least that many days at the as-of date; Branch > Class > Customer |
| PGIBR043 | Premium Income by Direct/Agent/Broker | approved premium by channel with commission in foreign and local currency; Source type > Branch > Class > Product |
| PGIBR084 | Marine Open Cover Certificate Report | certificates issued in a period with sum insured and premium in foreign and local currency and their claims, totalled per open cover |
| PGIBR085 | Risk Accumulation Report | sums insured of the risks in force on a date per accumulation zone |
| QTN-SUMMARY | Quotations Converted / Approved / Rejected | count per status and the quotations of each status, for quotations issued in the period |

All export to PDF, Excel and CSV. Claims reports PGIBR002, PGIBR012, PGIBR018, PGIBR023, PGIBR028,
PGIBR036 and PGIBR082 are described in [CLAIMS.md](CLAIMS.md); the UPR report belongs to
[ACTUARIAL_RESERVES.md](ACTUARIAL_RESERVES.md).

## 11. API (`/api/v1/underwriting`)

| Method | Path | Permission |
|---|---|---|
| GET | `/products?companyId`, `/products/{id}`, `/quotations?companyId&status&from&to`, `/quotations/{id}`, `/policies?companyId&branchId&status&productId&customerCode&q&fromDate&toDate&openCoverId&page&size`, `/policies/{id}`, `/policies/{id}/endorsements`, `/endorsements/{id}`, `/open-covers?companyId`, `/open-covers/{id}`, `/open-covers/{id}/certificates` | POLICY_VIEW |
| POST / PUT | `/products`, `/products/{id}` | POLICY_MAINTAIN |
| POST | `/quotations`, PUT `/quotations/{id}`, `/quotations/{id}/iterations`, `/quotations/{id}/submit`, `/quotations/{id}/convert` | POLICY_MAINTAIN |
| POST | `/policies/preview`, `/policies`, PUT `/policies/{id}`, `/policies/{id}/submit`, `/policies/{id}/discard`, `/policies/{id}/endorsements`, `/endorsements/{id}/submit`, `/endorsements/{id}/discard`, `/open-covers`, `/open-covers/{id}/certificates` | POLICY_MAINTAIN |
| POST | `/products/{id}/authorize`, `/quotations/{id}/approve`, `/quotations/{id}/reject`, `/quotations/expire?companyId&asOf`, `/policies/{id}/approve`, `/policies/{id}/reject`, `/endorsements/{id}/approve`, `/endorsements/{id}/reject`, `/open-covers/{id}/authorize` | POLICY_AUTHORIZE |

Policy and endorsement approvals take an optional `{ "accountingDate": "yyyy-MM-dd" }` (default
today). The `UNDERWRITER` role holds `POLICY_VIEW`, `POLICY_MAINTAIN` and `POLICY_AUTHORIZE`.

## 12. Demo data (FVI)

`V901` creates the parties (clients `C-000101`–`C-000204`, agents `A-0001` 15 % and `A-0002` 12.5 %,
brokers `B-0001` 20 % and `B-0002` 17.5 %, all with 10 % withholding; coinsurer `CO-0001`) and the
five underwriting rules above. `V910` grants `POLICY_AUTHORIZE` to the finance manager, so the
underwriter `uw` is the maker and `fmanager` the approver.

`UnderwritingDemoData` runs through the services (so journals, open items, notes and the event
register are real) and is skipped when the company already has policies:

- **Eight products**, all 1/365, DST 12.5 %, LGT 0.75 %, policy fee 250.00 (PA-IND 150.00):

  | Code | Line | Commission | VAT | FST | Premium tax | Open cover |
  |---|---|---:|---:|---:|---:|---|
  | FIRE-COM | FIRE | 20 % | 12 % | 2 % | – | no |
  | MOTOR-PC | MOTOR | 15 % | 12 % | – | – | no |
  | MARINE-CGO | MARINE | 17.5 % | 12 % | – | – | yes |
  | ENGG-CAR | ENGG | 15 % | 12 % | – | – | no |
  | CAS-CGL | CASUALTY | 15 % | 12 % | – | – | no |
  | PA-IND | PA | 20 % | 12 % | – | – | no |
  | HEALTH-GRP | HEALTH | 10 % | – | – | 2 % | no |
  | BONDS-SUR | BONDS | 15 % | 12 % | – | – | no |

- **150 policies** issued from 5 January to September 2026 over the branches HO, CEB and DVO,
  rotating products and channels (direct, agent, broker): 138 approved, 8 pending approval and 4
  drafts. Every 15th policy is coinsured at 60 % with `CO-0001` (every second of them led by the
  company); marine policies of Visayas Shipping Lines are in USD; some carry a 5 % discount or a
  10 % loading; a few short-term (180-day) policies are renewed.
- **Endorsements** on approved policies: additional premium, refunds, NIL changes, renewals and
  pro-rata cancellations.
- **13 quotations** in every status (2 expired by an expiry run on 22 September 2026, 2 converted,
  2 rejected, 2 approved, 3 pending, 2 draft).
- **One USD open cover** for Visayas Shipping Lines (2026, limit 2,000,000 per shipment,
  20,000,000 a year, rate 0.30 %) with eight approved certificates sailing every four weeks from
  10 February 2026.

Claims (`@Order(20)`) and reinsurance (`@Order(15)`) demo data build on this portfolio.

## 13. Open points and simplifications

- **Renewal overwrites the policy period.** The start of the original period is no longer stored
  once a policy is renewed; `CoverPeriodResolver` (and therefore actuarial reserves) takes the policy
  issue date as the start of the original period. The underwriting year is not changed by a renewal.
- **No authorization limit** on policy, endorsement or quotation approvals
  (`UnderwritingApprovalSource`); the maker-checker rule is the only approval control.
- **Quotation expiry** runs only through `POST /quotations/expire`; there is no scheduled job and no
  button on the Quotations screen, so lapsed quotations stay open until the call is made (conversion
  after expiry is refused in any case).
- **Pro-rata cancellation is 1/365 only**, whatever the product's UPR basis, and applies the
  period ratio to the whole current-period gross (endorsements made mid-term are returned at the
  same ratio); short-period scales and a refund of the policy fee are not supported.
- **Endorsements use the product's current tax rates** (and the policy's original commission rate):
  if a product's rates change after issue, later endorsements are taxed at the new rates.
- **One coinsurer per policy**; several coinsurers need one policy line per coinsurer or a later
  extension.
- **Marine certificates** are always written at 100 % without discount or loading, and each one
  bears the product's policy fee.
- **No reinsurance or claims without the ports**: when reinsurance or claims is not deployed, the
  registers show everything retained and no claims (by design, see section 9).
