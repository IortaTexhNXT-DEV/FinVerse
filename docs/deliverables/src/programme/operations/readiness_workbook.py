"""Production Readiness Checklist workbook of BIBS (programme level, BRD-00), from readiness.yaml.

Called by build_operations_pack.py; writes Programme/Operations/BIBS_Readiness_BRD-00_Production_Readiness_Checklist
_v<version>.xlsx with the sheets Checklist (one row per criterion, status drop-down), Summary (counts by area and
status, recalculated by Excel from the Checklist) and Gates (the review and go / no-go gates).
"""

from __future__ import annotations

import datetime as dt
from pathlib import Path
from typing import Any

import brand
from bdoi_xlsx import BdoiWorkbook, Column

STATUSES = ["OPEN", "IN PROGRESS", "DONE", "BLOCKED", "N/A"]
AREAS = ["Operational", "Security", "Performance", "Data", "Support", "Compliance", "Business"]
FIRST, LAST = 5, 600  # data rows of the Checklist sheet (header in row 4), with room for new rows


def _date(text: str) -> dt.date:
    return dt.datetime.strptime(text, "%d-%b-%Y").date()


def build_workbook(spec: dict[str, Any]) -> Path:
    version = str(spec["version"])
    wb = BdoiWorkbook("Production Readiness Checklist", doc_type="Readiness checklist", brd="BRD-00",
                      version=version, date=spec["date"],
                      subtitle="Operational, security, performance, data, support, compliance and business "
                               "readiness of BIBS for the January 2028 go-live")
    wb.legend = [("OPEN", "Not started or not yet evidenced"), ("IN PROGRESS", "Work under way, evidence partial"),
                 ("DONE", "Met; evidence filed"), ("BLOCKED", "Cannot be met without a decision or a dependency"),
                 ("N/A", "Not applicable, with the reason in Remarks")]
    wb.cover_notes = [
        "One row per readiness criterion. The owner updates the status and files the evidence reference; the BIBS "
        "Service Owner reviews the sheet weekly from 1 October 2027 and before each gate.",
        "A criterion is met only with its evidence. A criterion that is not met at its gate is either waived by the "
        "gate (recorded in Remarks with the approver) or blocks the gate.",
        "Gates: ORR 15-Dec-2027, PRR 17-Dec-2027, GNG-1 19-Dec-2027, GNG-2 29-Dec-2027, GNG-3 2-Jan-2028 18:00.",
    ]
    cols = [
        Column("id", "ID", 9, "Criterion identifier: area prefix and number"),
        Column("area", "Area", 13, "Readiness area", values=AREAS),
        Column("criterion", "Criterion", 46, "What must be true before go-live"),
        Column("measure", "Measure / threshold", 32, "How the criterion is judged met"),
        Column("evidence", "Evidence required", 28, "The record that proves it"),
        Column("owner", "Owner", 20, "Accountable role"),
        Column("due", "Due", 12, "Date by which the evidence is filed", kind="date"),
        Column("gate", "Gate", 9, "Review or go / no-go gate that checks it", values=[g[0] for g in spec["gates"]]),
        Column("status", "Status", 13, "Status of the criterion", values=STATUSES, status=True),
        Column("ref", "Evidence reference", 22, "Document, record or link of the evidence (filled in by the owner)"),
        Column("met", "Date met", 12, "Date the evidence was accepted (filled in by the owner)", kind="date"),
        Column("remarks", "Remarks", 28, "Waiver with approver, dependency or comment"),
    ]
    rows = [{**it, "due": _date(it["due"]), "ref": None, "met": None, "remarks": None} for it in spec["items"]]
    wb.sheet("Checklist", cols, rows, description="Production readiness criteria of BIBS with owner, evidence, gate "
                                                  "and status")
    rng = f"$B${FIRST}:$B${LAST}"
    srng = f"$I${FIRST}:$I${LAST}"
    summary = []
    for area in AREAS + ["Total"]:
        crit = (f"=COUNTA(Checklist!$A${FIRST}:$A${LAST})" if area == "Total"
                else f'=COUNTIF(Checklist!{rng},"{area}")')
        row = {"area": area, "count": crit}
        for s in STATUSES:
            row[s] = (f'=COUNTIF(Checklist!{srng},"{s}")' if area == "Total"
                      else f'=COUNTIFS(Checklist!{rng},"{area}",Checklist!{srng},"{s}")')
        summary.append(row)
    for i, row in enumerate(summary, start=FIRST):
        row["done_pct"] = f"=IF(B{i}-G{i}=0,0,ROUND(100*E{i}/(B{i}-G{i}),1))"
    scols = [Column("area", "Area", 16, "Readiness area"), Column("count", "Criteria", 10, "Number of criteria")]
    scols += [Column(s, s.title(), 12, f"Criteria with status {s}") for s in STATUSES]
    scols += [Column("done_pct", "Met (%)", 10, "Criteria met as a share of the applicable criteria")]
    wb.sheet("Summary", scols, summary, description="Counts by area and status, recalculated from the Checklist",
             freeze_first_column=False)
    gcols = [Column("gate", "Gate", 9, "Gate code"), Column("name", "Name", 30, "Name of the review or decision"),
             Column("when", "When", 22, "Date and time"), Column("who", "Decided by", 40, "Chair and members"),
             Column("rule", "Passes when", 50, "Rule of the gate")]
    wb.sheet("Gates", gcols, [list(g) for g in spec["gates"]], description="Readiness reviews and go / no-go gates")
    path = brand.out_path("BRD-00", "Operations",
                          brand.output_name("Readiness", "BRD-00", "Production Readiness Checklist", version, "xlsx"))
    return wb.save(path)
