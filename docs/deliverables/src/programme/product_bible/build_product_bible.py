"""Writes the BDOI Product Bible / LOB library workbook (data-migration product validation).

Core sheets: OOTB product set, BIBS product set, LOB status, Comparison, Migration mapping template, Migration checks;
then the library sheets (LOB summary, product library, covers and perils, clauses, rating and charges, underwriting
data dictionary, documents and KYC, glossary, sources and open questions).

Sources
  * product_bible_data.py / product_bible_library.py (this folder): LOBs, OOTB snapshot, comparison, library rows;
  * the BIBS catalogue configuration, read at every run through tools/deliverables/sql_facts.py (product lines,
    cover types, risk products, coverages / perils, clauses); Employee Benefits lines, package versions and
    business-line lookups are listed in product_bible_data.py.

Output
  docs/deliverables/out/Programme/Product_Bible/BIBS_Product_Bible_BDOI_LOB_Library_v1.0.xlsx

Usage
  python docs/deliverables/src/programme/product_bible/build_product_bible.py
"""

from __future__ import annotations

import sys
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
sys.path.insert(0, str(HERE))
import brand  # noqa: E402
import sql_facts  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

import product_bible_data as D  # noqa: E402
import product_bible_library as L  # noqa: E402

VERSION = "1.0"
DATE = "12-Oct-2026"
OUT = brand.OUT_DIR / "Programme" / "Product_Bible" / f"BIBS_Product_Bible_BDOI_LOB_Library_v{VERSION}.xlsx"

STATUS = ["In both", "OOTB only", "BIBS only", "Neither - market product to confirm"]
LOB_STATUS = ["Covered", "Partial", "Missing"]
MAP_STATUS = ["Mapped", "Needs new product", "Needs confirmation", "Retire"]
LEGACY = ["Ebix", "QPS", "Broker", "iSys", "Other"]
SRC_KIND = ["BDOI document", "BIBS configuration", "OOTB reference", "Market practice (to confirm with BDOI)"]

# Colours of the new status values (in-process only; brand.py is not changed).
for k, v in {"COVERED": "FIT", "IN BOTH": "FIT", "MAPPED": "FIT", "STANDARD": "FIT",
             "OOTB ONLY": "PARTIAL", "BIBS ONLY": "CONFIGURE", "NEEDS CONFIRMATION": "PARTIAL", "OPTIONAL": "CONFIGURE",
             "MISSING": "GAP", "NEITHER - MARKET PRODUCT TO CONFIRM": "GAP", "NEEDS NEW PRODUCT": "NEW",
             "RETIRE": "OUT", "BDOI DOCUMENT": "FIT", "BIBS CONFIGURATION": "CONFIGURE",
             "OOTB REFERENCE": "CONFIGURE", "MARKET PRACTICE (TO CONFIRM WITH BDOI)": "PARTIAL"}.items():
    brand.STATUS_COLOURS[k] = brand.STATUS_COLOURS[v]


class Workbook(BdoiWorkbook):
    def _print_setup(self, ws, title_rows):  # A3 landscape, fit to width
        super()._print_setup(ws, title_rows)
        ws.page_setup.paperSize = ws.PAPERSIZE_A3


def src_kind(text: str) -> str:
    t = text.lower()
    if t.startswith("market") or text == D.MKT:
        return SRC_KIND[3]
    if t.startswith("bibs"):
        return SRC_KIND[1]
    if t.startswith("ootb"):
        return SRC_KIND[2]
    return SRC_KIND[0]


def flag(v) -> str:
    return "Yes" if str(v).lower() in ("true", "t", "1", "yes") else "No"


# ------------------------------------------------------------------------------------------------ BIBS catalogue
def bibs_catalogue():
    lines = sql_facts.rows("cat_product_line")
    covers = sql_facts.rows("cat_cover_type", key=("line_code", "code"))
    products = sql_facts.rows("cat_product")
    coverages = sql_facts.rows("cat_coverage", key=("line_code", "code"))
    clauses = sql_facts.rows("cat_clause")
    return lines, covers, products, coverages, clauses


def packaged(p) -> str:
    if p["code"].startswith(("PAR", "MTR")) or p["code"] in ("CTP01", "CTP02", "PA001"):
        return "Yes"
    n = p["name"].lower()
    return "Yes" if ("program" in n or "package" in n) else "No"


def bibs_rows(cat):
    lines, covers, products, coverages, clauses = cat
    lob = D.BIBS_LINE_LOB
    rows = []
    src = "BIBS catalogue configuration"
    for ln in lines:
        rows.append(dict(lob=lob[ln["code"]], level="Product line", code=ln["code"], name=ln["name"], parent="-",
                         pk="n/a", rate="n/a", comm="n/a", minp="n/a",
                         detail=f"Risk item kind {ln['risk_item_kind']}; rating method {ln['rating_method']}", src=src))
    for code, name in D.BIBS_EB_LINES:
        rows.append(dict(lob=lob[code], level="Product line", code=code, name=name, parent="-", pk="n/a", rate="n/a",
                         comm="n/a", minp="n/a", detail="Employee Benefits line", src=src + " (Employee Benefits)"))
    for c in covers:
        rows.append(dict(lob=lob[c["line_code"]], level="Cover type", code=c["code"], name=c["name"],
                         parent=c["line_code"], pk="n/a", rate="n/a", comm="n/a", minp="n/a",
                         detail="Sub-line / type under the product line", src=src))
    for p in products:
        rows.append(dict(lob=lob[p["line_code"]], level="Product (risk code)", code=p["code"], name=p["name"],
                         parent=f"{p['line_code']} / {p['cover_type_code']}", pk=packaged(p),
                         rate=p["default_rate"] if p["default_rate"] not in (None, "null") else "per matrix",
                         comm=p["default_commission_rate"], minp=p["minimum_premium"],
                         detail=f"Payment gate {p['payment_gate']}; multi-year {flag(p['multi_year_allowed'])}"
                                f" (max {p['max_term_years']} yr); defaults to confirm with BDOI (Q01)",
                         src=src))
    for code, name, line in D.BIBS_EB_PRODUCTS:
        rows.append(dict(lob=lob[line], level="Product (risk code)", code=code, name=name, parent=line, pk="No",
                         rate="per plan", comm="per insurer", minp="0", detail="Employee Benefits product",
                         src=src + " (Employee Benefits)"))
    for c in coverages:
        rows.append(dict(lob=lob[c["line_code"]], level=f"Coverage / peril ({c['kind'].title()})", code=c["code"],
                         name=c["name"], parent=c["line_code"], pk="n/a", rate="n/a", comm="n/a", minp="n/a",
                         detail="Basic cover" if flag(c["basic"]) == "Yes" else "Optional / extension", src=src))
    for prod, ver, terms in D.BIBS_PACKAGES:
        for code, inc, txt in terms:
            rows.append(dict(lob=lob["MOTOR" if prod.startswith("MTR") else "PROPERTY"], level="Package coverage term",
                             code=code, name=f"{prod} {ver}", parent=prod, pk="Yes", rate="n/a", comm="n/a",
                             minp="n/a", detail=f"{inc}; {txt}", src=src + " (package versions)"))
    for c in clauses:
        ln = c["line_code"]
        rows.append(dict(lob=lob[ln] if ln and ln != "null" else "All lines", level=f"Clause ({c['kind'].title()})",
                         code=c["code"], name=c["title"], parent=ln if ln and ln != "null" else "All lines", pk="n/a",
                         rate="n/a", comm="n/a", minp="n/a", detail=c["wording"], src=src))
    for typ, code, name in D.BIBS_LOOKUPS:
        lob_of = {"FIRE": "Property", "MOTOR": "Motor", "MARINE": "Marine (Cargo)", "ENGG": "Engineering",
                  "CASUALTY": "Casualty", "PA": "Personal Accident", "HEALTH": "Health Insurance", "BONDS": "Bond"}
        rows.append(dict(lob=lob_of[code], level="Business-line lookup (MIS)", code=code, name=name,
                         parent="BUSINESS_LINE", pk="n/a", rate="n/a", comm="n/a", minp="n/a",
                         detail="Reporting / accounting classification, not a product line", src=src))
    order = {n: i for i, n in enumerate(D.LOB_NAMES + ["All lines"])}
    rows.sort(key=lambda r: order[r["lob"]])
    return rows


# ------------------------------------------------------------------------------------------------ sheets
def main() -> Path:
    cat = bibs_catalogue()
    wb = Workbook("BDOI Product Bible - LOB Library", doc_type="Product Bible", brd="BRD-00 / BRD-03 / BRD-13",
                  version=VERSION, date=DATE,
                  subtitle="BDOI products by line of business: OOTB vs BIBS catalogue, migration mapping and library")
    wb.cover_notes = [
        "Purpose: data-migration validation - every product BDOI uses today must have a home in the BIBS New Business "
        "product catalogue (Data Migration BRD BRID_MIG 3.1).",
        "The BIBS and OOTB product sets list configuration codes (catalogue reference data); they do not describe "
        "system behaviour.",
        "Source legend: 'BDOI document' rows come from BDOI's BRDs / FRS; 'Market practice (to confirm with BDOI)' rows "
        "are Philippine non-life market practice and need BDOI confirmation. No BDOI-specific fact is assumed.",
    ]
    wb.legend = [("In both", "Product in the OOTB set and in the BIBS catalogue"),
                 ("OOTB only", "Only in the OOTB platform set"), ("BIBS only", "Only in the BIBS catalogue"),
                 ("Neither - market product to confirm", "Market or BDOI-listed product with no code yet"),
                 ("Covered", "LOB has a line and its main products"), ("Partial", "LOB present, products missing"),
                 ("Missing", "No product line for the LOB")]

    lob_col = Column("lob", "BDOI LOB", 20, "One of BDOI's 15 lines of business", values=D.LOB_NAMES + ["All lines"])

    # 1 OOTB product set
    ootb = [dict(lob=r[0], level=r[1], code=r[2], name=r[3], parent=r[4], detail=r[5],
                 src="OOTB reference data (read 12-Oct-2026)") for r in D.OOTB_ROWS]
    order = {n: i for i, n in enumerate(D.LOB_NAMES + ["All lines"])}
    ootb.sort(key=lambda r: order[r["lob"]])
    wb.sheet("OOTB product set", [
        lob_col, Column("level", "Level", 18, "Line of business, product category, product, policy type, product template, cover, risk section or tax / charge"),
        Column("code", "OOTB code", 18, "Code in the OOTB platform reference data"),
        Column("name", "Name", 34, "Name in the OOTB platform"),
        Column("parent", "Parent", 18, "Line, product or template it belongs to"),
        Column("detail", "Detail", 50, "Classification (package / non-package, segment), status, rates as seeded"),
        Column("src", "Source", 26, "iNXT BrokerVerse OOTB platform, branch brokerverse-platform"),
    ], ootb, description="iNXT BrokerVerse OOTB platform product set by BDOI LOB (configuration codes, read-only reference)")

    # 2 BIBS product set
    brows = bibs_rows(cat)
    wb.sheet("BIBS product set", [
        lob_col, Column("level", "Level", 20, "Product line, cover type, product (risk code), coverage / peril, package coverage term, clause or business-line lookup"),
        Column("code", "BIBS code", 18, "Code in the BIBS catalogue"),
        Column("name", "Name", 44, "Name in the BIBS catalogue"),
        Column("parent", "Parent", 22, "Product line / cover type the row belongs to"),
        Column("pk", "Packaged", 10, "Package product (Yes / No); n/a for other levels"),
        Column("rate", "Default rate %", 11, "Default premium rate (%), defaults until BDOI's product matrix is loaded"),
        Column("comm", "Commission %", 11, "Default commission rate (%)"),
        Column("minp", "Min. premium (PHP)", 12, "Default minimum premium"),
        Column("detail", "Detail", 46, "Attributes: payment gate, multi-year, coverage kind, wording"),
        Column("src", "Source", 24, "BIBS New Business catalogue configuration"),
    ], brows, description="BIBS New Business catalogue by BDOI LOB: lines, cover types, products, coverages / perils, packages, clauses (configuration codes)")

    # 4 Comparison (rows 5..)
    comp = []
    for lob, prod, oc, bc, src, rem in D.COMPARISON:
        st = STATUS[0] if oc and bc else STATUS[1] if oc else STATUS[2] if bc else STATUS[3]
        comp.append(dict(lob=lob, prod=prod, oc=oc or "-", bc=bc or "-", st=st, src=L_src(src), kind=src_kind(L_src(src)), rem=rem))
    n_comp = len(comp)
    first, last = 5, 4 + n_comp

    # 3 LOB status (formulas over Comparison)
    srows = []
    for i, x in enumerate(D.LOBS):
        r = 5 + i
        o, oe, b, be = D.LOB_STATUS[x["name"]]
        rng = f"Comparison!$A${first}:$A${last}"
        st = f"Comparison!$E${first}:$E${last}"
        srows.append(dict(code=x["code"], lob=x["name"], ic=x["ic"], o=o, oe=oe, b=b, be=be,
                          n=f'=COUNTIF({rng},B{r})',
                          both=f'=COUNTIFS({rng},B{r},{st},"{STATUS[0]}")', oo=f'=COUNTIFS({rng},B{r},{st},"{STATUS[1]}")',
                          bo=f'=COUNTIFS({rng},B{r},{st},"{STATUS[2]}")', ne=f'=COUNTIFS({rng},B{r},{st},"{STATUS[3]}")'))
    tr = 5 + len(D.LOBS)
    srows.append(dict(code="Total", lob="All 15 LOBs", ic="-",
                      o=f'=COUNTIF(D5:D{tr-1},"Covered")&" covered"', oe="Covered LOBs in OOTB",
                      b=f'=COUNTIF(F5:F{tr-1},"Covered")&" covered"', be="Covered LOBs in BIBS",
                      n=f"=SUM(H5:H{tr-1})", both=f"=SUM(I5:I{tr-1})", oo=f"=SUM(J5:J{tr-1})", bo=f"=SUM(K5:K{tr-1})",
                      ne=f"=SUM(L5:L{tr-1})"))
    wb.sheet("LOB status", [
        Column("code", "LOB code (proposed)", 10, "Proposed BDOI LOB code, to confirm against legacy codes"),
        Column("lob", "BDOI LOB", 22, "BDOI line of business"),
        Column("ic", "IC class", 24, "Insurance Commission class / statement line (to confirm)"),
        Column("o", "OOTB status", 12, "Covered / Partial / Missing in the OOTB product set", values=LOB_STATUS, status=True),
        Column("oe", "OOTB evidence", 32, "What the OOTB set holds for the LOB"),
        Column("b", "BIBS status", 12, "Covered / Partial / Missing in the BIBS catalogue", values=LOB_STATUS, status=True),
        Column("be", "BIBS evidence", 36, "What the BIBS catalogue holds for the LOB"),
        Column("n", "Products compared", 10, "Rows of the Comparison sheet for the LOB (formula)", kind="number"),
        Column("both", "In both", 9, "Count with status In both (formula)", kind="number"),
        Column("oo", "OOTB only", 9, "Count with status OOTB only (formula)", kind="number"),
        Column("bo", "BIBS only", 9, "Count with status BIBS only (formula)", kind="number"),
        Column("ne", "Neither", 9, "Count with status Neither - market product to confirm (formula)", kind="number"),
    ], srows, description="Summary of the Comparison: BDOI's 15 LOBs, Covered / Partial / Missing in OOTB and in BIBS, counts per status")

    wb.sheet("Comparison", [
        lob_col, Column("prod", "Product", 36, "Product BDOI lists (FRS) or the market normally has under the LOB"),
        Column("oc", "In OOTB (code)", 26, "OOTB code(s); '-' when absent"),
        Column("bc", "In BIBS (code)", 40, "BIBS line / cover type (risk products) or coverage codes; '-' when absent"),
        Column("st", "Status", 18, "In both / OOTB only / BIBS only / Neither - market product to confirm", values=STATUS, status=True),
        Column("src", "Product source", 24, "Where the product comes from: BDOI document, BIBS configuration or market practice"),
        Column("kind", "Source type", 18, "Source legend", values=SRC_KIND, status=True),
        Column("rem", "Remarks / action for BDOI", 40, "Confirmation needed, product to add, legacy codes to map"),
    ], comp, description="One row per BDOI LOB and product: OOTB vs BIBS; gaps are flagged for BDOI confirmation, not as certain gaps")

    # 5 Migration mapping template
    lines, covers, products, coverages, clauses = cat
    cov_by_line = {}
    for c in coverages:
        cov_by_line.setdefault(c["line_code"], []).append(c["code"])
    mrows = []
    for p in products:
        mrows.append(dict(lob=D.BIBS_LINE_LOB[p["line_code"]], bline=p["line_code"], bprod=p["code"], bname=p["name"],
                          bpkg=f"{p['cover_type_code']} (packaged: {packaged(p)})",
                          bcov=", ".join(cov_by_line.get(p["line_code"], [])) or "per product (confirm)",
                          st="Needs confirmation", rem="BDOI to enter the legacy codes that map to this product"))
    for code, name, line in D.BIBS_EB_PRODUCTS:
        mrows.append(dict(lob=D.BIBS_LINE_LOB[line], bline=line, bprod=code, bname=name, bpkg="Plan per client",
                          bcov="per plan", st="Needs confirmation", rem="Map legacy EB plan codes"))
    for lob, prod, oc, bc, src, rem in D.COMPARISON:
        if not bc:
            mrows.append(dict(lob=lob, bline="(none)", bprod="(none)", bname=prod, bpkg="(none)", bcov="(none)",
                              st="Needs new product", rem=f"No BIBS product yet: {rem}"))
    for r in mrows:
        r.update(sys_="", llob="", lprod="", lname="", lpkg="", lcov="", ins="", cnt="")
    mrows.sort(key=lambda r: order[r["lob"]])
    wb.sheet("Migration mapping", [
        Column("sys_", "Legacy system", 11, "Legacy source system (BDOI to fill)", values=LEGACY),
        Column("llob", "Legacy LOB code", 11, "BDOI to fill"), Column("lprod", "Legacy product code", 12, "BDOI to fill"),
        Column("lname", "Legacy product name", 22, "BDOI to fill"), Column("lpkg", "Legacy package / plan code", 12, "BDOI to fill"),
        Column("lcov", "Legacy risk / coverage codes", 16, "BDOI to fill"), Column("ins", "Insurer", 14, "BDOI to fill"),
        Column("cnt", "Active policies count", 10, "BDOI to fill (in-force policies per legacy code)", kind="number"),
        lob_col, Column("bline", "BIBS line code", 18, "Target product line"),
        Column("bprod", "BIBS product code", 12, "Target BIBS product (risk code); (none) = product to add"),
        Column("bname", "BIBS product name", 36, "Name of the target product"),
        Column("bpkg", "BIBS package (cover type)", 20, "Cover type and package flag"),
        Column("bcov", "BIBS coverage / peril codes", 26, "Coverages / perils configured for the line"),
        Column("st", "Mapping status", 16, "Mapped / Needs new product / Needs confirmation / Retire", values=MAP_STATUS, status=True),
        Column("rem", "Remarks", 30, "Notes and actions"),
    ], mrows, description="Template for BDOI: legacy product codes (left, to fill) against the BIBS target product (right, pre-filled). Blank legacy cells are for BDOI input.", freeze_first_column=False)

    # 6 Migration checks
    wb.sheet("Migration checks", [
        Column("id", "Check ID", 8, "Check reference"), Column("chk", "Check", 24, "Name of the validation"),
        Column("rule", "Rule", 60, "Validation rule for the product reference data"),
        Column("src", "BRD reference", 18, "Data Migration BRD (BRD-13) BRID_MIG item or other BRD"),
        Column("sev", "Severity", 10, "Blocking / High / Medium", values=["Blocking", "High", "Medium"]),
        Column("how", "How verified", 40, "Evidence or report that proves the check"),
    ], [dict(zip(("id", "chk", "rule", "src", "sev", "how"), c)) for c in L.CHECKS],
        description="Validation rules for product reference data, aligned with the Data Migration BRD reconciliation requirements")

    # 7 LOB summary (library)
    lsum = []
    nprod = Counter(p["lob"] for p in L.P)
    for i, x in enumerate(D.LOBS):
        lsum.append(dict(code=x["code"], lob=x["name"], ic=x["ic"], desc=x["desc"], seg=x["seg"],
                         n=f'=COUNTIF(\'Product library\'!$A$5:$A${4 + len(L.P)},B{5 + i})', comp=x["compulsory"],
                         ins="Placed with IC-licensed non-life insurers (or HMOs for health); panel per package - see product matrix",
                         frs=x["frs"], ref=x["ref"]))
    wb.sheet("LOB summary", [
        Column("code", "LOB code (proposed)", 10, "Proposed code, to confirm"), Column("lob", "BDOI LOB", 20, "Line of business"),
        Column("ic", "IC class", 22, "IC class / statement line (to confirm)"), Column("desc", "Description", 44, "What the LOB covers"),
        Column("seg", "Typical clients / segments", 40, "Clients of a bank-affiliated broker (corporate, SME, retail, bank-related)"),
        Column("n", "Sub-products", 9, "Number of products in the Product library (formula)", kind="number"),
        Column("comp", "Compulsory?", 26, "Whether cover is required by law or by a lender / obligee"),
        Column("ins", "Key insurers note", 28, "Generic note; no insurer names"),
        Column("frs", "Name in BDOI FRS", 18, "Line of insurance as named in BDOI's Product Maintenance FRS"),
        Column("ref", "BRD / FRS references", 36, "BDOI documents that mention the LOB"),
    ], lsum, description="One row per BDOI line of business (library)")

    # 8 Product library
    prow = []
    for p in L.P:
        dft = L.LOB_DEFAULTS[p["lob"]]
        prow.append(dict(p, tax=dft["tax"], renew=dft["renew"], claims=dft["claims"], kind=src_kind(p["src"])))
    wb.sheet("Product library", [
        lob_col, Column("code", "Product code (proposed)", 11, "Proposed code; BIBS code in the name where one exists"),
        Column("name", "Product", 28, "Product / plan"), Column("desc", "Description", 30, "What it covers"),
        Column("interest", "Insured interest", 20, "Whose interest is insured"), Column("covers", "Standard covers / perils", 28, "Basic cover"),
        Column("ext", "Common extensions / endorsements", 26, "Usual add-ons"), Column("excl", "Key exclusions", 26, "Main exclusions"),
        Column("si", "Sum insured basis", 20, "Valuation / limit basis"), Column("rating", "Rating basis", 22, "Rate x SI, per head, per unit, minimum premium"),
        Column("ded", "Deductible / excess", 18, "Usual deductible practice"), Column("term", "Policy term", 16, "Usual period"),
        Column("uw", "Underwriting information required", 28, "Proposal fields"), Column("docs", "Documents required", 26, "Placement documents"),
        Column("tax", "Taxes / charges", 34, "DST / VAT or premium tax / LGT / FST rule for the LOB"),
        Column("renew", "Renewal considerations", 26, "What changes at renewal"), Column("claims", "Typical claims documents", 28, "Claims documents"),
        Column("src", "Source", 22, "BDOI document reference or market practice"),
        Column("kind", "Source type", 16, "Source legend", values=SRC_KIND, status=True),
    ], prow, description="One row per sub-product / plan per LOB; market rows to confirm with BDOI")

    # 9 Covers and perils
    short = {"Property": "prop", "Bond": "bond", "Personal Accident": "pa", "Motor": "mtr", "Health Insurance": "hlt",
             "Liability": "lia", "Engineering": "eng", "Electronic Equipment": "eei", "Others / Miscellaneous": "msc",
             "Marine (Cargo)": "mar", "Crime": "crm", "Marine Hull": "hul", "Property / Equipment Floater": "flt",
             "Aviation": "avn", "Casualty": "cas"}
    vals = ["Standard", "Optional", "n/a"]
    pcols = [Column("peril", "Peril / cover", 34, "Peril or cover")] + [
        Column(short[n], n, 10, f"Standard / Optional / n/a for {n}", values=vals, status=True) for n in D.LOB_NAMES]
    prow2 = []
    for peril, m in L.PERILS:
        r = {"peril": peril}
        for n in D.LOB_NAMES:
            r[short[n]] = m.get(n, "n/a")
        prow2.append(r)
    wb.sheet("Covers & perils", pcols, prow2, description="LOB x peril / cover: Standard, Optional or n/a (market practice; BDOI packages may differ)")

    # 10 Clauses
    wb.sheet("Clauses", [
        Column("lob", "LOB", 20, "LOB or All lines"), Column("cl", "Clause / endorsement", 36, "Name"),
        Column("t", "Type", 16, "Clause, warranty, exclusion, endorsement, condition, wording"),
        Column("p", "Purpose", 54, "What it does"), Column("src", "Source", 22, "BIBS clause library, BDOI document or market practice"),
        Column("kind", "Source type", 16, "Source legend", values=SRC_KIND, status=True),
    ], [dict(lob=a, cl=b, t=c, p=d, src=e, kind=src_kind(e)) for a, b, c, d, e in L.CLAUSES],
        description="Endorsements and clauses library per LOB")

    # 11 Rating and charges
    rr = []
    for x in D.LOBS:
        n = x["name"]
        dft = L.LOB_DEFAULTS[n]
        bases = sorted({p["rating"] for p in L.P if p["lob"] == n})
        rr.append(dict(lob=n, basis="; ".join(bases) or "confirm with BDOI",
                       minp="Per package (product matrix); not applied on endorsements or pro-rata transactions (NB BRD Appendix A)",
                       sp="Short term period rate = premium rate x % by months / days covered (NB BRD); scale percentages: confirm with BDOI / insurer",
                       tax=dft["tax"], fst="Yes (2% of net premium)" if n == "Property" else ("Confirm with BDOI / insurer" if n in ("Engineering", "Property / Equipment Floater", "Electronic Equipment") else "No"),
                       dst="12.5% of net premium; rounding: .01-.49 up to .50, .51-.99 up to 1.00 (NB BRD)" if n in ("Property", "Motor") else "12.5% assumed as Fire / Motor; confirm with BDOI / insurer",
                       comm=L.COMMISSION, src=dft["src_tax"], kind=src_kind(dft["src_tax"])))
    wb.sheet("Rating & charges", [
        lob_col, Column("basis", "Rating basis", 34, "How premium is computed"),
        Column("minp", "Minimum premium practice", 26, "Minimum premium rule"), Column("sp", "Short-period note", 26, "Cancellation scale"),
        Column("dst", "DST", 24, "Documentary stamp tax rule"), Column("tax", "Taxes and charges rule", 36, "DST, VAT / premium tax, LGT, FST"),
        Column("fst", "FST applies?", 14, "Fire service tax applicability"), Column("comm", "Commission / brokerage", 34, "Commission rule"),
        Column("src", "Source", 18, "BDOI document or market practice"),
        Column("kind", "Source type", 16, "Source legend", values=SRC_KIND, status=True),
    ], rr, description="Rating basis, minimum premium, short period, taxes and commission per LOB; amounts not in BDOI documents are flagged 'confirm'")

    # 12 Data dictionary
    wb.sheet("Data dictionary", [
        Column("f", "Field", 34, "Field name as in BDOI's Product Maintenance FRS where it exists"),
        Column("t", "Type", 16, "Data type"), Column("m", "Mandatory?", 14, "Mandatory rule (FRS: enforced by line and product type)"),
        Column("lov", "LOV / format", 34, "Allowed values"), Column("app", "LOB applicability", 40, "Lines the field applies to (FRS names)"),
        Column("src", "Source", 22, "BDOI FRS, BIBS field rules or market practice"),
        Column("kind", "Source type", 16, "Source legend", values=SRC_KIND, status=True),
    ], [dict(f=a, t=b, m=c, lov=d, app=e, src=s, kind=src_kind(s)) for a, b, c, d, e, s in L.DICT],
        description="Underwriting fields BIBS captures per LOB at quotation / placement")

    # 13 Documents and KYC
    survey = {"Property": "Risk inspection for large / high-hazard risks; photos", "Motor": "Vehicle inspection and photos for used cars",
              "Engineering": "Site survey for large projects", "Marine Hull": "Condition / valuation survey", "Aviation": "Airworthiness inspection",
              "Marine (Cargo)": "Pre-shipment survey for high-value cargo", "Electronic Equipment": "Equipment list check"}
    val = {"Property": "Appraisal / valuation report for high values; loan collateral appraisal (bank)", "Motor": "Sales invoice or market value guide",
           "Marine Hull": "Valuation survey (agreed value)", "Aviation": "Agreed value / valuation", "Engineering": "Contract value / bill of quantities"}
    dk = []
    for x in D.LOBS:
        n = x["name"]
        pdocs = sorted({p["docs"] for p in L.P if p["lob"] == n})
        dk.append(dict(lob=n, prop="; ".join(pdocs), kyc=L.KYC, sur=survey.get(n, "n/a"), val=val.get(n, "n/a"),
                       clm=L.LOB_DEFAULTS[n]["claims"], src=D.MKT if n not in ("Motor",) else "BIBS document rules (Motor IDF) + market practice",
                       kind=SRC_KIND[3]))
    wb.sheet("Documents & KYC", [
        lob_col, Column("prop", "Proposal and placement documents", 40, "Documents for quotation / placement"),
        Column("kyc", "KYC", 34, "Client due diligence"), Column("sur", "Inspection / survey", 24, "Survey practice"),
        Column("val", "Valuation", 24, "Valuation documents"), Column("clm", "Claims documents", 34, "Typical claims documents"),
        Column("src", "Source", 20, "Source"), Column("kind", "Source type", 16, "Source legend", values=SRC_KIND, status=True),
    ], dk, description="Documents and KYC per LOB")

    # 14 Glossary
    wb.sheet("Glossary", [Column("t", "Term", 18, "Term or abbreviation"), Column("d", "Meaning", 90, "Definition")],
             [dict(t=a, d=b) for a, b in L.GLOSSARY], description="Terms and abbreviations")

    # 15 Sources, assumptions and open questions
    sq = [dict(t=a, item=b, use=c) for a, b, c in L.SOURCES]
    sq += [dict(t="Assumption (market practice)", item=a, use="Confirm with BDOI") for a in L.ASSUMPTIONS]
    sq += [dict(t="Open question for BDOI", item=q, use="BDOI answer needed before the mapping is signed off") for q in L.QUESTIONS]
    wb.sheet("Sources & questions", [
        Column("t", "Type", 22, "Source document, programme deliverable, configuration, assumption or open question",
               values=["Source document", "Programme deliverable", "BIBS configuration", "OOTB reference", "Market practice",
                       "Assumption (market practice)", "Open question for BDOI"]),
        Column("item", "Item", 80, "Source, assumption or question"), Column("use", "Used for / action", 46, "Where it is used or what is needed"),
    ], sq, description="Every source used, every market-practice assumption and the open questions for BDOI")

    return wb.save(OUT)


def L_src(tag: str) -> str:
    return D.SRC.get(tag, tag)


if __name__ == "__main__":
    print(main())
