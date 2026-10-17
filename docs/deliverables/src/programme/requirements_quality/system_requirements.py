"""System requirements of the SRS, one set per BRD, each tagged with its BRD and traced to FRS IDs.

Read by build_requirements_quality.py (chapters of the SRS, the per-BRD SRS extracts of the packs and the System
requirements sheet of the traceability matrix). Nothing here is typed per requirement: the requirements come from

  * the FRS in BDOI's format of the re-baselined BRDs (BRD-02 Cashiering v3.4 and Remittance v1.0, BRD-03 v1.7,
    BRD-06 v1.3, BRD-07 Claims v1.0, BRD-11 v1.6): one system requirement per requirement of the FRS, traced to the
    requirement and its items, as the Conformance Register counts them;
  * the FRS of the other BRDs (BIBS format): one system requirement per functional requirement FR-xx-nnn, traced to
    the FR and the BRD requirement IDs it meets; BRD-00 gives the common requirements.

Functions the client deferred to Phase 2 (decisions of 17-Oct-2026: reinsurance and the partner portal) are tagged
Phase 2 from the gaps of the Conformance Register; co-insurance is in the current phase.
"""

from __future__ import annotations

import sys
from functools import lru_cache
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
CONFORMANCE = HERE.parent / "conformance"

# Module of each BRD: what it does (the FRS scope chapters, as delivered on 17-Oct-2026) and the FRS it is traced to.
MODULES: dict[str, tuple[str, str]] = {
    "BRD-00": ("Core Replacement (common requirements)",
               "Services that no function BRD owns: persona menus and role home pages, notifications, audit trail and "
               "master-data change log, standard, tailored and scheduled reports, documents from templates, lists of "
               "values, the insurer record with its co-insurance participation, MIS fields and products, sign-in, "
               "workflow routing, the insurer invoice batch, bulk processing, the Invoice Master List, "
               "multi-currency, the BDO brand and the SOA and service invoice at booking."),
    "BRD-01": ("New Business",
               "Client onboarding and KYC, tags and lifecycle rules; quotation and proposal (package and non-package "
               "through the TSU); Proposal Request Form, quotation slip and insurer comparatives; placement with the "
               "EOPT details of the client, hold cover, issuance and e-policy; booking with GL entry, service invoice "
               "and the co-insurance split of an invoice; bulk processing; New Business reports."),
    "BRD-02": ("Operations",
               "Invoice ledger and Invoice 360; Cashiering (AR and OR series, payment files, PDC warehouse, "
               "application by premium component, unapplied payments); Remittance to insurers (extraction on "
               "schedule with Excel and text extract files, settlement numbering and the copy to the shared folder, "
               "eligibility, approval, schedule, insurer OR return, early remittance incentive, special remittance, "
               "Remittance Settings under maker-checker); Adjustment and Cancellation; Production Reconciliation; "
               "Commission Receivables for direct payment; BIR 2307 certificates."),
    "BRD-03": ("Product Maintenance",
               "Package products: product structure and clause library, package request, insurer negotiation and "
               "comparatives, ManCom sign-off, set-up and validation, versions and rate schemes, expiry and renewal, "
               "incentive criteria; the insurer page with contacts, payment terms, bank and ledger accounts, "
               "co-insurance participation and change history."),
    "BRD-04": ("Collections",
               "Premium-receivable worklist per invoice, assignment, collector dispositions, promises to pay, "
               "installments, escalation, billing statements, collector side of unapplied payments, scheduled "
               "reversal files and daily reports."),
    "BRD-05": ("Accounting, Disbursement and ACSL",
               "Chart of accounts, posting and manual entries, closing and revaluation, bank reconciliation, service "
               "fee, report pack and BIR outputs; disbursement vouchers, instruments and BIR 2307 for suppliers; "
               "Marketing payment requests; ACSL reconciliation; business and system administration requests."),
    "BRD-06": ("Renewal",
               "Extraction of expiring accounts, governance rules and buckets, Marketing disposition and review, "
               "Processing and insurer round-trip, Renewal Advice and other letters, acceptance and progression to "
               "placement and booking, renewal of submitted policies."),
    "BRD-07": ("Claims",
               "Claims logging and monitoring for the client: dashboards per role, cover lookup and premium check "
               "with the claims authorization code, claim types, statuses and their types, locations by barangay, "
               "insurer claim numbers, reserve and settlement as reported, follow-ups, document checklist per claim "
               "type, motor details with accredited repair shops, return to the account officer, Formal Loss Advice "
               "with preview, worklist with sort and export, claims special remittance and the claims reports with "
               "the BDO Insure heading. The change requests CR-CLM-101 to 112 wait for BDOI's approval."),
    "BRD-08": ("Employee Benefits",
               "Group benefit programmes (HMO, group life, group personal accident): renewal start, broker on record, "
               "franchise and TOR, proposals and comparative sign-off, processing, billing and roster servicing, "
               "reports. The partner portal is deferred to Phase 2."),
    "BRD-09": ("Customer Servicing Facility",
               "Servicing workspace for the Contact Center: search and view of client, account, invoice and payment; "
               "contact-detail updates with audit; resend of renewal advices and e-policies; document upload; case "
               "management with routing to the unit of the account, SLA and notices; CSF reports."),
    "BRD-10": ("Sanction Screening",
               "Sanctions and PEP list intake, name matching, risk-profile tagging, cases with SLA and escalation, "
               "investigation, Unit Head and AML Committee approval, STR in the AMLC format, audit."),
    "BRD-11": ("User Access Maintenance",
               "Sign-in through Microsoft Entra ID; users and their group-profile membership orchestrated by "
               "UIDM-ISC; in BIBS the group-profile definitions under maker-checker, data scope, unlock and session "
               "end, segregation-of-duties rules, identity reconciliation, access reports and logs."),
    "BRD-12": ("Submitted Policies",
               "Intake of policies that bank borrowers submit, masterlist, sanitation and classification rules, policy "
               "review (IAAF), limits and TOR, renewal hand-off, handling fee, monitoring."),
    "BRD-13": ("Data Migration",
               "Migration Console: data object register, extracts and batches, validation, client matching, code maps, "
               "loads, reconciliation L1 to L5, legacy invoices after go-live, legacy inquiry, cut-over and run-off."),
}

# The packs in BDOI's format and the requirements of their FRS (BRD-02 has two FRS, one per pack).
PACKS = [
    ("BRD-02", "BRD-02_Operations_Cashiering", "Cashiering", lambda g: not g.startswith("FRID.RMT")),
    ("BRD-02", "BRD-02_Operations_Remittance", "Remittance", lambda g: g.startswith("FRID.RMT")),
    ("BRD-03", "BRD-03_Product_Maintenance", "", lambda g: True),
    ("BRD-06", "BRD-06_Renewal", "", lambda g: True),
    ("BRD-07", "BRD-07_Claims", "Claims", lambda g: True),
    ("BRD-11", "BRD-11_User_Access_Maintenance", "", lambda g: True),
]


@lru_cache(maxsize=1)
def _conformance():
    sys.path.insert(0, str(CONFORMANCE))
    import build_conformance as bc  # noqa: PLC0415
    assess = yaml.safe_load((CONFORMANCE / "assessment.yaml").read_text(encoding="utf-8"))
    deferred = {fr for g in assess.get("gaps") or [] if g.get("status") == bc.DEFERRED for fr in g.get("frs") or []}
    return bc.bdoi_baselines({}), deferred


def _items(items: list[tuple]) -> tuple[str, int]:
    ids = [i[0] for i in items if not (len(i) > 4 and i[4] == "In another FRS")]
    if not ids:
        return "-", 0
    return (ids[0] if len(ids) == 1 else f"{ids[0]} to {ids[-1]}"), len(ids)


def system_requirements(d: Any) -> dict[str, list[dict[str, Any]]]:
    """BRD -> its system requirements: sr, brd, text, trace, items, phase, frs."""
    key = id(d)
    if _CACHE.get("key") == key:
        return _CACHE["value"]
    baselines, deferred = _conformance()
    out: dict[str, list[dict[str, Any]]] = {}
    for brd in MODULES:
        rows = []
        tag = "COM" if brd == "BRD-00" else brd.replace("-", "")
        if brd in baselines:
            version = baselines[brd]["frs"].split()[-1]
            for g in baselines[brd]["groups"]:
                if brd == "BRD-02":
                    frs = ("Operations Remittance FRS v1.0" if g["id"].startswith("FRID.RMT")
                           else f"Operations Cashiering FRS {baselines[brd]['frs'].split()[1]}")
                else:
                    frs = f"{MODULES[brd][0]} FRS {version}"
                frs += " (BDOI's format)"
                if g["status"] == "Withdrawn":
                    continue
                span, n = _items(g["items"])
                if n == 0:
                    continue
                rows.append({"brd": brd, "text": g["title"], "trace": g["id"], "items": f"{span} ({n})",
                             "phase": "Phase 2" if g["id"] in deferred else "Current phase", "frs": frs,
                             "group": g["id"]})
        else:
            for r in d.frreg:
                if r["brd"] != brd or r["state"] == "Removed from scope":
                    continue
                rows.append({"brd": brd, "text": r["title"], "trace": r["fr"], "items": r["brd_ids"] or "-",
                             "phase": "Phase 2" if r["fr"] in deferred else "Current phase",
                             "frs": f"{MODULES[brd][0]} {'Handbook' if brd == 'BRD-13' else 'FRS'} "
                                    f"{r['frs_doc'].rsplit('(', 1)[-1].rstrip(')')}", "group": r["fr"]})
        for i, r in enumerate(rows, 1):
            r["sr"] = f"SR-{tag}-{i:03d}"
        out[brd] = rows
    _CACHE.update(key=key, value=out)
    return out


_CACHE: dict[str, Any] = {}


def baseline_text(rows: list[dict[str, Any]]) -> str:
    return "; ".join(dict.fromkeys(r["frs"] for r in rows)) or "-"
