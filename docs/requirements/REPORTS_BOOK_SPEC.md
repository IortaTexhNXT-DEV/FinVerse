# Insurance (GI) Reports – Implementation Specification

Source: *Annexure 2(c) Reports Book – GI* (78 pages). This file is the traceability baseline used to
build the insurance reports in iNXT FinVerse. Report codes are kept identical to the source so
business users can recognise them. Where the source did not state a rule explicitly, the rule
adopted by FinVerse is marked **(FinVerse rule)**.

## 0. Conventions
- Range parameters `X From/To` are optional; blank = all values.
- Date/as-on parameters and option parameters are mandatory (defaults supplied by the UI).
- Money columns are shown at **100%** and **Our Share** where coinsurance applies.
- Net Premium = Gross Premium + Loading − Discount.
- Claim estimate / paid types: 1 Payment, 2 Recovery, 3 Reversal of Payment, 4 Reversal of Recovery.
- "Include Expense Provision" – when unchecked only loss reserves are included.
- Output header: company, title, generating branch, user, run date, report code, page n of m,
  echo of parameters, group headers, sub-totals, grand total, `*** End of Report ***`.
- Output formats: on-screen, PDF, Excel (XLSX), CSV.

## 1. Underwriting
| Code | Title | Key parameters | Columns (order) | Group | Rules |
|---|---|---|---|---|---|
| PGIBR042 | List of Quotations Pending Approval | Branch, Department(Class), Product, Customer, Broker ranges; Issue date from/to; As-of date; Pending days | Quotation No, Iteration, Issue Date, Customer, Period From, Period To, Our Share %, 100% SI, Our SI, 100% Gross, Our Gross, 100% Disc, Our Disc, 100% Loading, Our Loading, Charges, 100% Net, Our Net, Brokerage | Branch > Class > Customer | Status = pending; pending days 0 = all, else as-of − issue ≥ days **(FinVerse rule: ≥)** |
| PGIBR005 | Premium Register with DN Number | ranges; Based on (Issue/Accounting/Approval/Period-from); date from/to; Account option | Policy No, Endt No, Insured, Customer, Issue Date, Endt Date, Period, Approval Date, Our Share %, 100%/Our SI, Gross, Disc, Loading, Net, Policy Fee, Other charges, Broker, Broker Commission, Broker CN No, Customer DN No | Branch > Class > Product | DN = debit note of policy; CN = broker credit note |
| PGIBR040 | Transactions Pending Approval | ranges; issue date from/to | Policy No, Endt count, Assured, Created By, Period, Our Share %, SI, Gross, Disc, Loading, Net, Charges, Commission | Branch > Class > Product | Status draft / pending approval |
| PGIBR003 | Daily Production Report | ranges; Based on Issue/Approval; date from/to | Date, No of Policies, No of Endorsements, 100% Net, Our Net | Branch > Class > Product | Counts per day |
| PGIBR015 | Premium Register (General) | as PGIBR005 | Serial, Invoice No, Txn Date, Policy, Endt, Assured, Period From/To, Agent, Our Share %, SI, Gross, Disc, Loading, Net, Coins %, Coins share of net, Net of coins, Taxes & charges, Broker commission, Treaty RI, Net retention, FAC premium | Branch > Class > Product > UW Year | Treaty RI = QS + Surplus ceded; Net retention = retained premium |
| PGIBR016 | Premium Register Summary | ranges; Based on; date from/to | Product, SI, Gross, Disc, Loading, Net, Taxes & charges, Broker commission, FAC, Treaty RI, Net retention | Branch > Class > UW Year | Same RI rules as PGIBR015 |
| PGIBR043 | Premium Income by Direct/Agent/Broker | ranges; source type; approval date from/to | Policy, Endt, Assured, Our Share %, SI, Gross, Charges, Commission %, Currency, Commission FC, Commission LC | Source type > Branch > Class > Product | |
| PGIBR025 | Broker Income Statement | Branch, Product, Customer, Broker ranges; approval date from/to | Policy, Endt, Assured, Our Share %, 100% Gross, Our Gross, Broker %, Broker Commission | Branch > Broker | Source type = Broker |
| PGIBR013 | List of Policies Due for Renewal | ranges; expiry date from/to | Policy, Assured, Period From/To, Customer, No of Claims, Intermediary, Our Share %, SI, Gross, Disc, Loading, Net, Policy Fee, Commission, FAC, Net Claim Amount, Claim Ratio | Branch > Class > Product > UW Year | Claim ratio = net claim / our net premium × 100 |
| PGIBR027 | Schedule of Shipment under Open Policy | Open policy no; certificate approval date from/to | Certificate No, LC No, Bank, Vessel, Sail Date, Port From, Port To, B/L No, B/L Date, Basis of valuation, Our Share %, SI, Net Premium, Charges, Commission | none | Marine certificates under open cover |
| QTN-SUMMARY | Quotations Converted / Approved / Rejected | ranges; period from/to | Summary: status → count; Detail: Status, Quotation, Insured, Issue Date, Validity (days), SI, Premium | Status | |

## 2. Claims
| Code | Title | Key parameters | Columns | Group | Rules |
|---|---|---|---|---|---|
| PGIBR002 | Claims Settled Statement | ranges; paid date from/to; include expense | Claim, Policy, Loss Date, Assured, Nature of Loss, Estimate Payment, Estimate Recovery, Total Estimate, Paid Payment, Paid Recovery, Total Paid, Closed | Branch > Class > Product | Estimate payment = type 1+3, recovery = 2+4 |
| PGIBR018 | Claims Outstanding | ranges; as-on; include expense | Claim, Policy, Assured, Loss Date, Payment Estimate, Recovery Estimate, Paid, Recovered, Payment O/S, Recovery O/S, Net O/S | Branch > Class > Product > UW Year | O/S = estimate − paid; net = payment O/S − recovery O/S; closed claims excluded |
| PGIBR036 | Client-wise Outstanding Claims | ranges; as-on | Claim, Status, Policy, Loss Date, Assured, Estimate, Paid, O/S | Branch > Customer | |
| PGIBR012 | Claims Ratio by Class of Business | ranges; policy expiry from/to | Policy, Period, Policy Status (New/Renewed), Assured, Our Share %, SI, Net Premium, Paid, O/S, Total, Claim Ratio | Branch > Class > Product | Ratio = (paid+O/S)/our net premium × 100 when premium ≠ 0 |
| PGIBR028 | Claims Ratio – Policy | ranges; approval date from/to | Policy, Period, Status, Broker, Commission, Our Share %, Net Premium, Net Claim, Ratio %, Opening O/S, Closing O/S, Paid, Release | Branch > Class > Product > Customer | Net claim = paid + closing O/S − opening O/S; release = opening − paid − closing |
| PGIBR023 | Production / Claims Analysis | ranges; approval date from/to | Product, New, Renewal, Additional, Refund, Total Gross (100% and Our), Claim Estimate, Claim Paid, Outstanding | Branch > Class | New = base policy; Renewal = renewal; Additional = positive endorsement; Refund = negative endorsement |
| PGIBR082 | LPO Issued (OD and TP) | Branch, Garage, Product, Cover ranges; loss date from/to | Claim, LPOs issued, Gross LPO, Discount, Net LPO | Branch > Garage > Product | Net = gross − discount |

## 3. Reinsurance
| Code | Title | Key parameters | Columns | Group | Rules |
|---|---|---|---|---|---|
| PGIR0637 | Reinsurance Premium Register | Branch, Class, treaty type; RI accounting date from/to; allocation status | Serial, Policy, Endt, Product, Risk, SI, Our SI, PML, Our PML, Gross, Net, Our Share %, FAC SI, FAC Premium, Retention, Retention %, QS, QS %, Surplus, Surplus %, Cession No | UW Year > Branch > Class | Only policies with RI allocation |
| PGIR0692 | FAC Premium Register | ranges; FAC approval date from/to | Policy, Endt, Issue Date, Placement No, Participant, Placement %, Placement SI, Placement Premium, Participant %, Participant SI, Participant Premium, Commission %, Commission | Branch > Class > Product | |
| PGIR0693 | Policies Pending for FAC Closing | ranges; approval date from/to | Policy, Approval Date, Risk, Risk SI, Risk Premium, Prov. FAC %, Prov. FAC SI, Prov. FAC Premium, Placement No, Placed SI, Placed Premium, Placement %, Status, FAC Commission | Branch > Class > Product | Placement % = placed SI / FAC SI × 100 |
| PGIR0638 | Reinsurance Claims Paid Register | ranges; RI claim date from/to; allocation status | Serial, Policy, Claim, Loss Date, Risk, SI, Our Share %, FAC Amount, FAC %, XOL, Claim Paid, Retention, Retention %, QS, QS %, Surplus, Surplus % | UW Year > Branch > Class | |
| PGIR0696 | Treaty Claims Paid | ranges; loss date from/to | Product, Claim, Recovery Paid %, FAC, Retention, QS, Surplus | Branch > Class | Recovery % = recovery / paid × 100 |
| PGIR0639 | Reinsurance Claims Outstanding Register | ranges; as-on; allocation status | Serial, Policy, Claim, Risk, Our Share %, SI, FAC Amount, FAC %, XOL, Claim O/S, FAC O/S, Loss Date, Retention, QS, Surplus (+%) | UW Year > Branch > Class | O/S = estimate − paid |
| RI-SOA | Statement of Account for Reinsurer | Treaty year, treaty, quarter, statement date, reinsurer | Income / Outgo lines: premium ceded, commission, levy, losses paid, recoveries, premium reserve retained/released, interest, O/S loss reserve retained/released, sub-total, balance due, total, amount in words | per treaty/participant | Balance = income − outgo, placed on smaller side |

## 4. Processing / actuarial
| Code | Title | Key parameters | Columns | Rules |
|---|---|---|---|---|
| PGIBR072 | UPR Summary Report | ranges; Earned/Unearned; UPR processed date; Detail/Summary | Policy, Period, Approval Date, Total Units (days), Earned Units, Unearned Units, Gross Premium, Commission, Earned / Unearned Premium & Commission, Treaty premium/commission earned/unearned, FAC premium/commission earned/unearned | 1/365 (daily pro-rata): unearned = amount × unexpired days / total days |
| PGIBR079 | IBNR Processing Report | processed date; Summary Y/N | Branch, Line of business, Source type, Product, IBNR Rate, Base (earned premium), IBNR Amount, RI portion | IBNR = base × rate %; RI portion = IBNR × ceded share |
| PGIBR080 | OSLR Processing Report | processed date | Branch, Line of business, Source type, Product, OSLR Amount | OSLR = Σ(approved estimate − paid) of open claims |
| PGIBR074 | Surplus / Mudharabah Payment | ranges; approval date from/to; posted/unposted | Branch, Policy, Insured, Product, Expiry, Gross, Discount, Loading, Commission, Claims, Applicable contribution, Retakaful, Tax, Payable | Applicable = gross − discount + loading − commission − claims **(FinVerse rule)** |
| PGIBR085 | Risk Accumulation Report | Accumulation group (zone), as-on | S.No, Accumulation zone, Occupation, Policy, Endt, Period (days), SI, Product | Σ SI per group |
| PGIBR084 | Marine Open Cover Certificate Report | ranges; date from/to | Certificate, Period, Insured, SI FC/LC, Premium FC/LC, Claim, Loss Date, Reserve, Paid, O/S | Open cover totals |
| RISK-PROFILE | Risk Profile (RI, UW year wise) | UW year from/to; date from/to | SI band, No of Risks, SI & premium split Risk/Retention/QS/Surplus/FAC, paid claims count & split, O/S claims count & split | Risk = Retention + QS + Surplus + FAC |
