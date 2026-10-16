"""Developer Specification (DSP) of a BRD pack, in BDOI's template. Written by build_pack.py from the content module
of the BRD (for BRD-11: brd11_pack.py) and the facts read from the sources (tools/deliverables/api_facts.py,
code_facts.py, schema_facts.py): screens and their service calls, service operations with their request checks,
messages, jobs, audit records and the permissions held by each persona.
"""

from __future__ import annotations

import re
from pathlib import Path

import api_facts as af
import code_facts as cf
import schema_facts as sf
from pack_common import DocMeta, PackDoc, finish

STATUS = {"Business rule": "422", "Field check": "400", "Not found": "404", "Duplicate": "409",
          "Request check": "400"}
SPECIAL_STATUS = {"AUTHENTICATION_FAILED": "401", "LOGIN_RATE_LIMITED": "429", "RESET_RATE_LIMITED": "429",
                  "SIGN_IN_CHECK_UNAVAILABLE": "503", "SIGN_IN_UNAVAILABLE": "503"}
WRAPPERS = ("PageResponse", "List", "Set", "ResponseEntity", "Optional", "Collection")
JAVA_TYPES = {"String": "Text", "Long": "Number", "long": "Number", "Integer": "Number", "int": "Number",
              "boolean": "Yes/No", "Boolean": "Yes/No", "BigDecimal": "Decimal", "LocalDate": "Date",
              "Instant": "Date-time", "OffsetDateTime": "Date-time", "LocalDateTime": "Date-time"}


def _type(t: str) -> str:
    t = t.strip()
    m = re.match(r"(?:List|Set|Collection)<(.+)>$", t)
    if m:
        inner = _type(m.group(1))
        return "List of " + inner[:1].lower() + inner[1:]
    if t in JAVA_TYPES:
        return JAVA_TYPES[t]
    vals = af.enum_values(t)
    if vals:
        return "Code (" + ", ".join(v.upper() for v in vals[:6]) + (", ..." if len(vals) > 6 else "") + ")"
    return "Object"


def _inner(t: str) -> tuple[str, str]:
    """PageResponse<AccessRequestResponse> -> ("Page of", "AccessRequestResponse")."""
    prefix = ""
    while True:
        m = re.match(r"(\w+)<(.+)>$", t.strip())
        if not m or m.group(1) not in WRAPPERS:
            break
        prefix = {"PageResponse": "Page of ", "List": "List of ", "Set": "List of "}.get(m.group(1), prefix)
        t = m.group(2)
    return prefix, t.strip()


def _clean(text: str) -> str:
    """Description of an operation without the internal references of the sources."""
    text = re.sub(r"\s*\((?:[^()]*?(?:FR-|NFR|BRD|PMADD|BRNB|UQ|design|DESIGN|section|SCIM RFC|RFC)[^()]*)\)", "",
                  text)
    text = re.sub(r"\s*\([^()]*\b[A-Z]{2,} \d+(?:\.\d+)+[^()]*\)", "", text)
    text = re.sub(r"\s*\([^()]*\b(?:GET|POST|PUT|PATCH|DELETE) /[^()]*\)", "", text)
    text = re.sub(r"\b[A-Z][a-z]+(?:[A-Z][a-z]+){2,}\b", "", text)
    text = text.replace("BrokerVerse", "BIBS")
    return " ".join(text.split()).rstrip(" ;,")


def _human(fn: str) -> str:
    return re.sub(r"(?<=[a-z])([A-Z])", r" \1", fn).capitalize()


def input_fields(K) -> dict[str, list[list[str]]]:
    """Input fields per screen of the UX Design: field, type, mandatory, default, rule or requirement."""
    out: dict[str, list[list[str]]] = {}
    for s in K.SCREENS:
        if not s["fields"]:
            continue
        rows = []
        for f in s["fields"]:
            rf = next(x for x in af.record_fields(f["dto"][0]) if x.name == f["dto"][1]) if f["dto"] else None
            dtype = _type(rf.type) if rf else f.get("type", "Text")
            if dtype.startswith("Code ("):
                dtype = "Choice"
            if rf and rf.length and dtype == "Text":
                dtype = f"Text, up to {rf.length.split('-')[-1]}"
            mand = "Yes" if (rf and rf.mandatory) or "Required" in f["client"] or "Mandatory" in f["client"] \
                else "No"
            rows.append([f["label"], dtype, mand, f.get("default", "-"), f.get("rule", s["frum"]),
                         f["msg"] or "-"])
        out[K.UX_INPUTS[s["id"]]] = rows
    return out


class Facts:
    def __init__(self, K):
        self.K = K
        java = af.JAVA
        self.ops = [o for o in af.operations(files=[java / c for c in K.CONTROLLERS]) if o.path not in K.OMIT_OPS
                    and not o.path.startswith("auth/sso/saml")]
        shared = af.operations(files=[java / c for c in K.SHARED_CONTROLLERS])
        self.ops += [o for o in shared if any(af.match_operation(c, [o]) for s in K.SCREENS if "page" in s
                                              for c in self._raw_calls(s))]
        msgs = cf.backend_messages(K.MESSAGE_PACKAGES)
        keep = []
        for m in msgs:
            if m.package == "system" and not m.cls.startswith(K.SETTING_CLASSES):
                continue
            if m.code in ("code",) or not re.fullmatch(r"[A-Z][A-Z0-9_]+", m.code):
                continue
            if any(w in m.cls for w in TEXT["omit_classes"]):
                continue
            keep.append(m)
        self.messages: dict[str, list] = {}
        for m in keep:
            self.messages.setdefault(m.code, [])
            if m.text not in [x.text for x in self.messages[m.code]]:
                self.messages[m.code].append(m)
        self.messages.setdefault("VALIDATION_FAILED", [cf.Message("VALIDATION_FAILED", "<field>: <check message>",
                                                                  "Request check", "", "", "", 0, "")])
        self.calls = {}
        for s in K.SCREENS:
            self.calls[s["id"]] = self._screen_calls(s)
        self.grants = self._grants()

    def _raw_calls(self, s) -> list[af.ClientCall]:
        feat, page = s["page"]
        calls = af.screen_calls(feat, page)
        for f2, p2 in s.get("extra", []):
            calls += [c for c in af.screen_calls(f2, p2) if c not in calls]
        fix = getattr(self.K, "fix_call", None)  # a content module corrects or drops calls the reader cannot resolve
        if fix:
            calls = [x for x in (fix(c) for c in calls) if x is not None]
        return calls

    def _screen_calls(self, s) -> list[tuple[str, af.Operation | None]]:
        out = []
        if "calls" in s:
            for verb, path, fn in s["calls"]:
                c = af.ClientCall("auth", fn, verb, path)
                out.append((fn, af.match_operation(c, self.ops)))
            return out
        for c in self._raw_calls(s):
            out.append((c.fn, af.match_operation(c, self.ops)))
        return out

    def _grants(self) -> dict[str, set[str]]:
        roles = [r for _, r in self.K.PERSONA_ROLES]
        rows = sf.rows("select r.code, rp.permission from sec_role r join sec_role_permission rp on rp.role_id = r.id "
                       f"where r.code in ({', '.join(repr(x) for x in roles)})")
        out: dict[str, set[str]] = {r: set() for r in roles}
        for row in rows:
            out[row["code"]].add(row["permission"])
        return out

    def status(self, code: str) -> str:
        if code in SPECIAL_STATUS:
            return SPECIAL_STATUS[code]
        return STATUS.get(self.messages[code][0].kind, "422") if code in self.messages else "422"

    def text(self, code: str) -> str:
        return " / ".join(m.text for m in self.messages.get(code, []))

    def check(self) -> list[str]:
        problems = []
        for s in self.K.SCREENS:
            for fn, op in self.calls[s["id"]]:
                if op is None:
                    problems.append(f"{s['id']}: the call {fn} reaches no service operation")
            for f in s["fields"]:
                for code in f["codes"]:
                    if code not in self.messages:
                        problems.append(f"{s['id']} {f['label']}: unknown message code {code}")
                if f["dto"]:
                    names = [x.name for x in af.record_fields(f["dto"][0])]
                    if f["dto"][1] not in names:
                        problems.append(f"{s['id']}: {f['dto'][0]} has no field {f['dto'][1]}")
            for code in s["codes"]:
                if code not in self.messages:
                    problems.append(f"{s['id']}: unknown message code {code}")
        for rule in self.K.RULE_ENFORCEMENT:
            for code in re.findall(r"\b[A-Z][A-Z0-9]+(?:_[A-Z0-9]+)+\b", rule[2]):
                if code not in self.messages:
                    problems.append(f"{rule[0]}: unknown message code {code}")
        return problems


PREFIXES: tuple[str, ...] = ()
CLIENT_CREDENTIAL = "UIDM-ISC client credential"

# Wording of the developer specification that depends on the BRD; a content module overrides an entry with DSP_TEXT.
TEXT = {
    "requirement_ids": "FRUM, BR-UA",
    "screen_ids": "SCR-UA",
    "interfaces": "SCIM 2.0 and OpenID Connect detail",
    "integration_prefix": "integration/",
    "service_ops": "The SCIM operations are detailed in the Interface Specification.",
    "status_note": "Separation-of-duties rules and UAM settings follow the maker-checker statuses of the data "
                   "dictionary (sheet Status models).",
    "jobs_note": "Synchronise Now on Identity Sync starts UAM_IDENTITY_RECONCILIATION.",
    "audit_note": "Group-profile changes also keep their own history (who did what, when).",
    "security_note": "The SCIM service accepts only the UIDM-ISC client credential issued by the API gateway.",
    "omit_classes": ("Simulator", "Retention", "Mfa"),
}


def _perm(op: af.Operation) -> str:
    if op.permissions:
        mine = [p for p in op.permissions if p.startswith(PREFIXES)] if PREFIXES else op.permissions
        rest = len(op.permissions) - len(mine)
        return ", ".join(mine) + (f" (or one of {rest} report permissions of other modules)" if rest else "")
    if "isAuthenticated" in op.permission:
        return "Signed-in user"
    if "permitAll" in op.permission:
        return "Public"
    if TEXT["integration_prefix"] and op.path.startswith(TEXT["integration_prefix"]):
        return CLIENT_CREDENTIAL
    return op.permission or "-"


def _route(path: str) -> str:
    return path.lstrip("/")


def write(K, out: Path, tmp: Path, pdf: bool = True) -> tuple[Path, int, dict]:
    global PREFIXES
    PREFIXES = K.PERMISSION_PREFIXES
    TEXT.update(getattr(K, "DSP_TEXT", {}))
    facts = Facts(K)
    problems = facts.check()
    if problems:
        raise SystemExit("developer specification: " + "; ".join(problems))
    meta = DocMeta(file=K.file_name("DSP"), cover=K.DOCS["DSP"]["cover"], header_left=K.DOCS["DSP"]["header_left"],
                   header_right=K.HEADER_RIGHT, doc_no=K.DOCS["DSP"]["doc_no"], version=K.version("DSP"),
                   date_us=K.DATE_US, created_us=K.DATE_US,
                   revisions=[(K.DATE, K.version("DSP"), "First issue. Describes the screens, service operations, "
                               "validations, rules, states, jobs, audit records and permissions of the release of "
                               f"{K.DATE}.", K.PREPARED)])
    d = PackDoc(meta)
    screen_map = {s["id"]: s for s in K.SCREENS}
    menu = {sc.path.lstrip("/"): sc for _, _, sc in cf.all_screens()}

    # 1 Introduction -------------------------------------------------------------------------------------------
    d.h1("Introduction")
    d.para(f"This document tells a developer or tester how {K.NAME} works: each screen, the service operations it "
           "calls, the checks on each field, where each business rule is enforced, the status models, the jobs, "
           "the audit records and the permission behind each action. Requirements and rules are stated in the FRS; "
           "this document refers to them by ID.")
    d.kv([("Release described", K.DATE),
          ("Requirements", f"{K.file_name('FRS')[:-5]} ({TEXT['requirement_ids']})"),
          ("Screens", f"{K.file_name('UXD')[:-5]} ({TEXT['screen_ids']})"),
          ("Interfaces", f"{K.file_name('ICD')[:-5]}: {TEXT['interfaces']}"),
          ("Data", f"{K.file_name('DDI')[:-5]}: tables, columns, lists, parameters"),
          ("Paths", "Relative to the service base address of the environment, version 1. Path variables are "
                    "written {name}."),
          ("Error answer", "Status code, message code and message text; a field check also returns the field. "
                           "Status 400: request check (code VALIDATION_FAILED). 401: not signed in or sign-in "
                           "refused. 403: permission missing or outside the user's data access. 404: record not "
                           "found. 409: duplicate, or the record changed since it was read. 422: business rule.")],
         caption="Conventions")

    # 2 Module overview ----------------------------------------------------------------------------------------
    d.h1("Module Overview")
    d.table(["Component", "What it does", "Detail"], K.COMPONENTS, [2.6, 5.0, 1.8], caption="Components")
    rows = []
    for s in K.SCREENS:
        sc = menu.get(s["route"])
        perm = s.get("permission") or (f"{sc.permission}" + (f" (or {', '.join(sc.also)})" if sc.also else "")
                                       if sc else s.get("permission_override", ("-",))[0])
        rows.append([s["id"], s["name"], s["route"], perm, s["uxd"]])
    d.table(["ID", "Screen", "Route", "Opened with", "UXD"], rows, [1.3, 2.0, 2.5, 3.0, 1.4],
            caption="Screens (route relative to the application address)")
    area_rows = []
    for area, prefixes, what in K.AREAS:
        n = sum(1 for o in facts.ops if o.path.startswith(prefixes))
        area_rows.append([area, what, str(n)])
    d.table(["Area", "Covers", "Operations"], area_rows, [2.4, 5.6, 1.2], caption="Service operations by area")
    jobs = [j for j in af.jobs(K.PACKAGES) if j.name not in K.JOB_OMIT]
    d.table(["Job", "Schedule (Manila time)", "What it does"],
            [[j.name, j.time_pht, K.JOBS.get(j.name, _clean(j.summary))] for j in jobs], [3.0, 1.8, 4.4],
            caption="Scheduled jobs")

    # 3 Screens ------------------------------------------------------------------------------------------------
    d.h1("Screen Specifications")
    d.para("For each screen: route and permission, the data loaded on opening, each action with the operation it "
           "calls, the state rules, the field checks and the messages. The service checks every field again, "
           "whatever the screen checked; a service check of - means the screen check only.")
    for s in K.SCREENS:
        d.h2(f"{s['id']} {s['name']}")
        sc = menu.get(s["route"])
        if s.get("permission"):
            perm = s["permission"]
        elif "permission_override" in s:
            p0, also = s["permission_override"]
            perm = p0 + (f" (or {', '.join(also)})" if also else "")
        else:
            perm = sc.permission + (f" (or {', '.join(sc.also)})" if sc.also else "")
        d.kv([("Route", s["route"]), ("Opened with", perm), ("UX Design", s["uxd"]), ("Requirements", s["frum"]),
              ("Data loaded", s["loads"])])
        calls = facts.calls[s["id"]]
        reads = [(fn, op) for fn, op in calls if op and op.verb == "GET" and fn not in K.ACTION_LABELS]
        acts = [(fn, op) for fn, op in calls if op and (op.verb != "GET" or fn in K.ACTION_LABELS)]
        if reads:
            d.table(["Data", "Operation", "Permission"],
                    [[K.DATA_LABELS.get(fn, _human(fn)), f"{op.verb} {op.path}", _perm(op)] for fn, op in reads],
                    [2.6, 4.2, 2.6],
                    caption=f"{s['name']}: data loaded")
        if acts:
            d.table(["Action", "Operation", "Permission", "Result"],
                    [[K.ACTION_LABELS.get(fn, _human(fn)), f"{op.verb} {op.path}", _perm(op), _clean(op.summary)]
                     for fn, op in acts], [1.7, 3.1, 2.3, 2.5], caption=f"{s['name']}: actions")
        d.para("State rules:", bold_lead=None)
        d.bullets(s["states"])
        if s["fields"]:
            frows = []
            field_codes: list[str] = []
            for f in s["fields"]:
                rf = None
                if f["dto"]:
                    rf = next(x for x in af.record_fields(f["dto"][0]) if x.name == f["dto"][1])
                dtype = _type(rf.type) if rf else f.get("type", "Text")
                if dtype.startswith("Code ("):
                    dtype = "Code"
                fmt = ", ".join(c for c in (rf.checks if rf else []) if not c.startswith(("required", "each")))
                mand = "Yes" if (rf and rf.mandatory) or "Required" in f["client"] or "Mandatory" in f["client"] \
                    else "No"
                server = "; ".join(rf.checks) if rf and rf.checks else "-"
                msgs = [f"\"{f['msg']}\"" ] if f["msg"] else []
                msgs += f["codes"] + (["VALIDATION_FAILED"] if rf and rf.messages else [])
                field_codes += [c for c in f["codes"] if c not in field_codes]
                frows.append([f["label"], dtype + (f"; {fmt}" if fmt else ""), mand, f["lov"], f["client"], server,
                              "\n".join(msgs) or "-"])
            d.table(["Field", "Type, length, format", "Req.", "List or source", "Screen check", "Service check",
                     "Messages"], frows, [1.6, 1.5, 0.6, 1.5, 1.8, 1.6, 2.0], caption=f"{s['name']}: field checks")
            codes = field_codes + [c for c in s["codes"] if c not in field_codes]
        else:
            codes = s["codes"]
        if codes:
            d.table(["Code", "Status", "Message"], [[c, facts.status(c), facts.text(c)] for c in codes],
                    [3.4, 0.9, 5.0], caption=f"{s['name']}: messages")

    # 4 Service operations ----------------------------------------------------------------------------------------
    d.h1("Service Operations")
    d.para("Every operation of the module, by area. Request fields list the checks of the service; response fields "
           "name what is returned. The business codes of an operation are listed with the screen that calls it "
           "(section 3) and in Annex A. " + TEXT["service_ops"])
    for area, prefixes, _ in K.AREAS:
        ops = [o for o in facts.ops if o.path.startswith(prefixes)]
        if not ops:
            continue
        d.h2(area)
        rows = []
        for o in ops:
            params = ", ".join(f"{p.name}{'' if p.required else '?'}" for p in o.params if p.where == "query"
                               and not p.type.endswith("Query"))
            q = [p for p in o.params if p.type.endswith("Query")]
            if q:
                params = ", ".join(f"{x.name}?" for x in af.record_fields(q[0].type)) or params
            pre, inner = _inner(o.returns)
            resp = "-" if o.returns in ("void", "ResponseEntity<Void>") else (
                pre + ", ".join(f.name for f in af.record_fields(inner, near=o.file)) if af.record_fields(
                    inner, near=o.file) else pre + ("file" if "File" in inner or "byte" in inner or "Resource" in
                                                   inner else "result"))
            req = "-"
            if o.body:
                fields = af.record_fields(o.body, near=o.file)
                req = "; ".join(f"{f.name}{'*' if f.mandatory else ''}" for f in fields) if fields else "SCIM resource"
            names = resp.split(", ")
            if len(names) > 10:
                resp = ", ".join(names[:10]) + f" and {len(names) - 10} more"
            rows.append([f"{o.verb} {o.path}", _perm(o), (f"Query: {params}\n" if params else "") + f"Body: {req}",
                         resp, _clean(o.summary)])
        d.table(["Operation", "Permission", "Request", "Response", "Purpose"], rows,
                [2.6, 1.9, 1.9, 2.0, 1.8], caption=f"{area}: operations (* mandatory)")
    d.h2("Request Field Checks")
    seen, rows = set(), []
    for o in facts.ops:
        if not o.body or o.body in seen:
            continue
        seen.add(o.body)
        for f in af.record_fields(o.body, near=o.file):
            if f.name.lower().startswith("password") or "password" in f.name.lower():
                checks = "; ".join(f.checks) or "-"
            else:
                checks = "; ".join(f.checks) or "-"
            rows.append([o.verb + " " + o.path, f.name, _type(f.type), "Yes" if f.mandatory else "No", checks,
                         "; ".join(f.messages) or "-"])
    d.table(["Operation", "Field", "Type", "Mand.", "Service check", "Explicit message"], rows,
            [2.6, 1.5, 1.6, 0.6, 1.9, 1.6], caption="Request fields of the operations with a body")

    # 5 Business rules -----------------------------------------------------------------------------------------
    d.h1("Business Rule Implementation")
    d.table(["Rule", "Screen", "Service (message code)", "Data or job"], [list(r) for r in K.RULE_ENFORCEMENT],
            [1.2, 2.6, 3.0, 2.6], caption="Where each business rule of the FRS is enforced")

    # 6 Workflows ----------------------------------------------------------------------------------------------
    d.h1("Status Models and Transitions")
    for entity, (enum, rows) in K.WORKFLOWS.items():
        codes = af.enum_values(enum)
        missing = [c for c in codes if c not in {r[0] for r in rows}]
        if missing:
            raise SystemExit(f"status model {entity}: codes without a row: {missing}")
        d.table(["Code", "Status", "Reached by", "Next"], [list(r) for r in rows], [2.2, 1.8, 3.4, 2.4],
                caption=f"{entity}")
    d.para(TEXT["status_note"])

    # 7 Jobs -------------------------------------------------------------------------------------------------------
    d.h1("Jobs and Schedules")
    d.table(["Job", "Schedule (UTC)", "Manila time", "What it does"],
            [[j.name, j.cron_utc, j.time_pht, K.JOBS.get(j.name, _clean(j.summary))] for j in jobs],
            [2.8, 1.6, 1.4, 3.6], caption="Jobs (cron: second minute hour day month weekday)")
    d.para("Every run is recorded with its start, end, outcome and counts, and can be started at once from "
           "Operations › Jobs by a user with the job permission. " + TEXT["jobs_note"])

    # 8 Audit ------------------------------------------------------------------------------------------------------
    d.h1("Audit Records")
    rows = []
    for e in af.audit_events(K.PACKAGES):
        if e.entity in K.AUDIT_OMIT:
            continue
        label = K.AUDIT_LABELS.get(e.entity)
        if label is None:
            raise SystemExit(f"audit record without a business name: {e.entity}")
        rows.append([label, e.action, K.ACTION_WORDS.get(e.action, e.action.capitalize())])
    rows = sorted({tuple(r) for r in rows})
    d.table(["Record", "Action", "Meaning"], [list(r) for r in rows], [3.2, 2.2, 3.8],
            caption="Audit records written by the module")
    d.para("Each record holds the user, time (UTC), source address, record and, for a change, the values before and "
           "after. " + TEXT["audit_note"] + " The audit trail is insert-only.")

    # 9 Security ---------------------------------------------------------------------------------------------------
    d.h1("Security: Permission per Action")
    personas = [p for p, _ in K.PERSONA_ROLES]
    rows = []
    for o in facts.ops:
        if not o.permissions:
            continue
        holders = [p for p, r in K.PERSONA_ROLES if facts.grants[r] & set(o.permissions)]
        rows.append([f"{o.verb} {o.path}", ", ".join(o.permissions), ", ".join(holders) or "-"])
    d.table(["Operation", "Permission (any of)", "Personas holding it"], rows, [3.6, 2.8, 2.8],
            caption="Permission checked by the service, and the walkthrough personas that hold it")
    d.table(["Persona", "Group profile", f"{K.SHORT} permissions"],
            [[p, r, ", ".join(sorted(facts.grants[r]))] for p, r in K.PERSONA_ROLES], [2.4, 2.4, 4.4],
            caption="Personas of the walkthroughs")
    d.para("The menu shows a screen only to a user holding its permission; the service checks the permission again "
           "on every call, and every list and record is limited to the user's data access. " + TEXT["security_note"])

    # Annex --------------------------------------------------------------------------------------------------------
    d.h1("Annex A: Message Catalogue")
    rows = [[c, facts.status(c), facts.text(c)] for c in sorted(facts.messages)]
    d.table(["Code", "Status", "Message (values in angle brackets are filled in)"], rows, [3.4, 0.9, 5.0],
            caption="Messages of the module")
    target = out / meta.file
    n = finish(d, target, tmp, pdf=pdf)
    return target, n, {"screens": len(K.SCREENS), "operations": len(facts.ops), "messages": len(facts.messages),
                       "personas": len(personas)}
