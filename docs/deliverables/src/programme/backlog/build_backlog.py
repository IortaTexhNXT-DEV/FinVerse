"""Builds the BIBS user story backlog for Jira (BRD-00, programme level).

Decision of the BIBS Product Owner of 9 October 2026: the FRS keeps the user-story view and the storyboard index as
signed appendices, and the working backlog lives in Jira. This script produces the backlog from the same sources as
those appendices, so that the two cannot drift:

  * every FRS of BRD-00 to BRD-12 and the Data Migration Handbook of BRD-13 (``fr`` blocks with their numbered
    acceptance criteria, the appendix "User-story view", the appendix "Storyboard index", the clarification tables);
  * the test case files of each BRD (brdNN_cases.yaml: the test conditions and cases of each FR);
  * backlog.yaml in this folder: the drop of each epic, the splits and merges of FRs into stories, the user stories
    derived from an FR that no BRD row names, the priority and estimation rules, the Definition of Ready and Done;
  * JIRA_IMPORT_GUIDE.md in this folder: the source of the import guide, with <!-- bl:... --> tables and {{...}}
    counts filled here.

Outputs (docs/deliverables/out/Programme/Backlog/)
  * BIBS_Backlog_BRD-00_User_Story_Backlog_Drop-0_v1.0.xlsx, -Drop-1, -Drop-2: Summary, one sheet per BRD (epic rows
    then their stories), Coverage, BRD ID trace, Definition of Ready and Done, Field mapping;
  * jira/BIBS_Jira_Import_Drop-0.csv, -Drop-1, -Drop-2: the Jira Cloud CSV import files (UTF-8, one header row, Issue
    ID and Parent for the links, Labels repeated as columns);
  * BIBS_Backlog_BRD-00_Jira_Import_Guide_v1.0.docx.

Usage
  python docs/deliverables/src/programme/backlog/build_backlog.py            # all files, guide with page numbers
  python docs/deliverables/src/programme/backlog/build_backlog.py --no-pdf   # guide without the TOC page pass
  python docs/deliverables/src/programme/backlog/build_backlog.py --check    # sources and written files only
"""

from __future__ import annotations

import argparse
import csv
import re
import sys
from collections import Counter, OrderedDict, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
from bdoi_docx import BdoiDocument, load_source, meta_from, output_path, render_body  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

CONFIG = yaml.safe_load((HERE / "backlog.yaml").read_text(encoding="utf-8"))
VERSION = str(CONFIG["version"])
DATE = str(CONFIG["date"])
OUT = brand.out_dir("BRD-00", "Backlog")
JIRA_DIR = OUT / "jira"
GUIDE_SRC = HERE / "JIRA_IMPORT_GUIDE.md"
FILE_DROPS = ["Drop 0", "Drop 1", "Drop 2"]
VERSIONS = ["Drop 0", "Drop 1", "Drop 2", "Phase 2"]
BRDS = ["BRD-00"] + list(brand.BRD_NAMES)
FIRST_ID = {"Drop 0": 10001, "Drop 1": 20001, "Drop 2": 30001}
PLACEHOLDER = re.compile(r"^<!--\s*bl:(\w+)\s*(.*?)-->\s*$")
TOKEN = re.compile(r"\{\{(\w+)\}\}")


# ============================================================================================ source model

@dataclass
class Fr:
    id: str
    brd: str
    data: dict[str, Any]
    section: str          # section title in the chapter Functional requirements (the capability)
    number: str           # its number in the FRS (4.5)
    raw: str              # the YAML text of the block

    @property
    def title(self) -> str:
        return clean(self.data.get("title", ""))

    @property
    def acs(self) -> list[str]:
        return [clean(a) for a in as_list(self.data.get("acceptance"))]


@dataclass
class Frs:
    brd: str
    path: Path
    front: dict[str, Any]
    docname: str
    frs: "OrderedDict[str, Fr]" = field(default_factory=OrderedDict)
    usv: list[dict[str, Any]] = field(default_factory=list)          # user-story view rows
    storyboard: list[dict[str, Any]] = field(default_factory=list)   # storyboard index rows
    clr: list[dict[str, Any]] = field(default_factory=list)          # clarifications
    sections: dict[str, str] = field(default_factory=dict)          # section title -> number
    conditions: dict[str, list[tuple[str, int]]] = field(default_factory=dict)  # FR -> [(TC id, cases)]

    @property
    def label(self) -> str:
        kind = "Data Migration Handbook" if self.brd == "BRD-13" else "FRS"
        return f"{kind} {self.brd} {brand.BRD_NAMES.get(self.brd, 'Core Replacement')} v{self.front.get('version')}"


def as_list(value: Any) -> list[Any]:
    if value is None:
        return []
    return value if isinstance(value, list) else [value]


def clean(value: Any) -> str:
    """Plain business text: no Markdown emphasis or code marks, single spaces."""
    if isinstance(value, list):
        value = "; ".join(clean(v) for v in value)
    text = str(value if value is not None else "").replace("**", "").replace("`", "")
    return re.sub(r"\s+", " ", text).strip()


def fr_refs(text: str) -> list[str]:
    """Every FR ID of a text, with the short forms expanded: 'FR-NB-014, 043, 051', 'FR-OP-090 to 099'."""
    out: list[str] = []
    for m in re.finditer(r"FR-([A-Z]+)-(\d{3})((?:\s*(?:,|to|and|-|–)\s*\d{3}\b)*)", text):
        prefix, last = m.group(1), int(m.group(2))
        ids = [f"FR-{prefix}-{last:03d}"]
        for sep, num in re.findall(r"(,|to|and|-|–)\s*(\d{3})\b", m.group(3)):
            n = int(num)
            ids += ([f"FR-{prefix}-{k:03d}" for k in range(last + 1, n + 1)] if sep in ("to", "-", "–")
                    else [f"FR-{prefix}-{n:03d}"])
            last = n
        out += [i for i in ids if i not in out]
    return out


def table_cells(line: str) -> list[str]:
    return [c.strip() for c in line.strip().strip("|").split("|")]


def frs_source(brd: str) -> Path:
    folder = brand.src_dir(brd)
    found = sorted(folder.glob("HANDBOOK_*.md" if brd == "BRD-13" else "FRS_*.md"))
    return found[0]


def read_frs(brd: str) -> Frs:
    path = frs_source(brd)
    front, lines = load_source(path)
    target = output_path(front, path)
    if brd == "BRD-13":
        target = target.with_name(brand.output_name("Handbook", brd, brand.BRD_NAMES[brd],
                                                    str(front.get("version")), "docx"))
    doc = Frs(brd, path, front, target.name)
    numbers = [0, 0, 0]
    h1 = h2 = ""
    region = ""
    fence: str | None = None
    buf: list[str] = []
    header: list[str] = []
    for line in lines:
        if fence is not None:
            if line.startswith("```"):
                if fence in ("fr", "requirement"):
                    raw = "\n".join(buf)
                    data = yaml.safe_load(raw)
                    doc.frs[data["id"]] = Fr(data["id"], brd, data, h2, doc.sections.get(h2, ""), raw)
                fence = None
            else:
                buf.append(line)
            continue
        if line.startswith("```"):
            fence, buf = line[3:].strip(), []
            continue
        m = re.match(r"^(#{1,3}) (.+?)\s*$", line)
        if m:
            level, title = len(m.group(1)), m.group(2)
            numbered = not title.endswith("{-}")
            title = title.replace("{-}", "").strip()
            if numbered:
                numbers[level - 1] += 1
                for i in range(level, 3):
                    numbers[i] = 0
            num = ".".join(str(n) for n in numbers[:level]) if numbered else ""
            if level == 1:
                h1, h2 = title, ""
                region = ("usv" if "User-story view" in title else "sb" if "Storyboard index" in title else "")
                header = []
            elif level == 2:
                h2 = title
                if "functional requirements" in h1.lower():
                    doc.sections.setdefault(title, num)
            continue
        if not line.startswith("| "):
            continue
        cells = table_cells(line)
        if re.match(r"^CLR-[A-Z]+-\d+$", cells[0]) and len(cells) >= 5:
            doc.clr.append({"id": cells[0], "topic": cells[1], "proposed": cells[2], "decision": cells[4],
                            "frs": set(fr_refs(" ".join(cells[1:4])))})
            continue
        if region and (not header or cells[0] == header[0]):
            header = cells
            continue
        if region == "usv":
            row = dict(zip(header, cells))
            doc.usv.append({"brd_id": cells[0], "story": clean(cells[1]), "fr_text": cells[2],
                            "frs": fr_refs(cells[2]), "acs": cells[3], "tcs": cells[4] if len(cells) > 4 else "",
                            "row": row})
        elif region == "sb":
            row = dict(zip(header, cells))
            frs = fr_refs(row.get("FR", ""))
            if not frs:
                continue
            doc.storyboard.append({
                "frame": clean(row.get("Frame", "")), "persona": clean(row.get("Persona", "")),
                "screen": clean(row.get("Screen") or row.get("Screen or document") or ""),
                "figure": clean(row.get("Figure") or row.get("Screenshot") or ""),
                "slide": clean(row.get("UX deck slide") or row.get("UX slide") or ""), "frs": frs})
    # the FR sections are numbered when their FRs are read; store the section numbers on the FRs read before
    for fr in doc.frs.values():
        fr.number = doc.sections.get(fr.section, fr.number)
    if brd == "BRD-13":
        catalogue = yaml.safe_load((path.parent / "pack" / "catalogue.yaml").read_text(encoding="utf-8"))
        for row in catalogue.get("proposals") or []:
            if str(row[0]).startswith("CLR-"):
                doc.clr.append({"id": row[0], "topic": clean(row[1]), "proposed": clean(row[2]),
                                "decision": clean(row[4]), "frs": set(fr_refs(" ".join(str(c) for c in row[1:4])))})
    for fr in doc.frs.values():
        for cid in re.findall(r"CLR-[A-Z]+-\d+", fr.raw):
            for c in doc.clr:
                if c["id"] == cid:
                    c["frs"].add(fr.id)
    read_conditions(doc)
    return doc


def read_conditions(doc: Frs) -> None:
    """Test conditions of each FR (TC-<code>-<nnn>.<i>) and the number of cases of each, from brdNN_cases.yaml."""
    if doc.brd == "BRD-00":
        return
    nn = doc.brd[-2:]
    raw = yaml.safe_load((doc.path.parent / f"brd{nn}_cases.yaml").read_text(encoding="utf-8"))
    for fr_id, entry in (raw.get("frs") or {}).items():
        entry = entry or {}
        counts = Counter(c.get("c") for c in entry.get("cases") or [])
        number = fr_id[3:]
        doc.conditions[fr_id] = [(f"TC-{number}.{i}", counts.get(i, 0))
                                 for i in range(1, len(entry.get("conditions") or []) + 1)]


# ============================================================================================ backlog model

@dataclass
class Story:
    brd: str
    frs: list[str]                       # FR IDs (one, or two merged)
    acs: list[tuple[str, int, str]]      # (FR, AC number, text)
    summary: str
    story: str
    part: str = ""                       # "1 of 2" for a split FR
    issue_id: int = 0
    epic: "Epic | None" = None
    version: str = ""
    version_note: str = ""
    points: int = 0
    priority: str = ""
    labels: list[str] = field(default_factory=list)
    brd_ids: list[str] = field(default_factory=list)
    tcs: str = ""
    frs_ref: str = ""
    gwt: list[str] = field(default_factory=list)
    rules: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)
    links: list[str] = field(default_factory=list)
    background: list[str] = field(default_factory=list)
    description: str = ""


@dataclass
class Epic:
    brd: str
    capability: str
    number: str
    file_drop: str
    version: str
    reason: str
    stories: list[Story] = field(default_factory=list)
    issue_id: int = 0
    description: str = ""

    @property
    def name(self) -> str:
        return f"{CONFIG['brds'][self.brd]['short']} - {self.capability}"

    @property
    def points(self) -> int:
        return sum(s.points for s in self.stories)


@dataclass
class Backlog:
    docs: dict[str, Frs]
    epics: list[Epic]
    usv_index: dict[str, list[tuple[str, dict[str, Any]]]]   # FR -> [(BRD of the row, row)]
    errors: list[str] = field(default_factory=list)

    def stories(self, file_drop: str | None = None) -> list[Story]:
        return [s for e in self.epics if file_drop in (None, e.file_drop) for s in e.stories]

    def epics_of(self, file_drop: str) -> list[Epic]:
        return [e for e in self.epics if e.file_drop == file_drop]


def priority_of(text: str) -> str:
    low = text.lower()
    for key, level in CONFIG["priority"]:
        if low.startswith(key.lower()) or (key.lower() in low and key not in ("High", "Medium", "Low")):
            return level
    return "Medium"


PLURALS = {"users": "User", "approvers": "Approver", "handlers": "Handler", "tls": "TL", "auditors": "Auditor",
           "administrators": "Administrator", "validators": "Validator", "officers": "Officer"}


def persona_labels(actor: str) -> list[str]:
    """Labels of the personas of an FR actor: 'Marketing AO; client HR user' -> Marketing_AO, Client_HR_User.
    Generic actors (every user, any user with a permission) and permission codes give no label; BIBS is System."""
    out: list[str] = []
    text = re.sub(r"\([^)]*\)", "", actor)
    for part in re.split(r"[;,/]| and | or ", text):
        part = re.split(r"\b(?:with|holding|who|receiving)\b", part)[0].strip()
        words = [w for w in part.split() if not re.fullmatch(r"[A-Z0-9]+(?:_[A-Z0-9]+)+", w)]
        if not words or len(words) > 4 or words[0].lower() in ("every", "all", "any", "authorised", "the", "a"):
            continue
        if words[0].startswith("CLR-"):
            continue
        words = [PLURALS.get(w.lower(), w[0].upper() + w[1:] if w.islower() else w) for w in words]
        label = re.sub(r"[^A-Za-z0-9_-]", "", "_".join(words))
        label = "System" if label in ("BIBS", "System", "Job") else label
        if label and label not in out:
            out.append(label)
    return out[:3]


def drop_rule(brd: str, capability: str) -> tuple[str, str, str]:
    """(file drop, fix version, reason) of an epic."""
    spec = CONFIG["drops"].get(brd, {})
    primary = brand.BRD_DROP[brd] if brd != "BRD-00" else spec.get("default", "Drop 1")
    version = (spec.get("sections") or {}).get(capability, primary)
    reason = spec.get("reason", "") if version != primary or brd == "BRD-00" else ""
    file_drop = version if version in FILE_DROPS else primary
    return file_drop, version, reason


def build_model() -> Backlog:
    docs = {brd: read_frs(brd) for brd in BRDS}
    usv_index: dict[str, list[tuple[str, dict[str, Any]]]] = defaultdict(list)
    errors: list[str] = []
    all_frs = {fid for d in docs.values() for fid in d.frs}
    for brd, d in docs.items():
        for row in d.usv:
            for fid in row["frs"]:
                if fid not in all_frs:
                    errors.append(f"{brd} {row['brd_id']}: {fid} is not an FR of any FRS")
                usv_index[fid].append((brd, row))
    epics: list[Epic] = []
    cuts = CONFIG.get("epic_cuts") or {}
    for brd in BRDS:
        d = docs[brd]
        groups: "OrderedDict[str, list[Fr]]" = OrderedDict()
        cut = cuts.get(brd, {})
        for fr in d.frs.values():
            name = fr.section
            for part in cut.get(fr.section, []):
                if fr.id in part["frs"]:
                    name = part["name"]
            groups.setdefault(name, []).append(fr)
        for name, frs in groups.items():
            file_drop, version, reason = drop_rule(brd, name)
            epic = Epic(brd, name, frs[0].number, file_drop, version, reason)
            epic.stories = stories_of(frs, d, usv_index, errors)
            for s in epic.stories:
                s.epic = epic
            epics.append(epic)
    model = Backlog(docs, epics, usv_index, errors)
    number_issues(model)
    for e in model.epics:
        for s in e.stories:
            complete_story(model, s)
        e.description = epic_description(model, e)
    return model


def stories_of(frs: list[Fr], doc: Frs, usv_index: dict, errors: list[str]) -> list[Story]:
    out: list[Story] = []
    splits = CONFIG.get("splits") or {}
    derived = CONFIG.get("derived_stories") or {}
    for fr in frs:
        story = story_text(fr, usv_index, derived)
        if not story:
            errors.append(f"{fr.id}: no row of the user-story view and no derived story in backlog.yaml")
        if fr.id in splits:
            parts = splits[fr.id]
            for k, part in enumerate(parts, start=1):
                acs = [(fr.id, n, fr.acs[n - 1]) for n in part["acs"]]
                out.append(Story(fr.brd, [fr.id], acs, part["summary"], part.get("story", story),
                                 part=f"{k} of {len(parts)}"))
            covered = sorted(n for p in parts for n in p["acs"])
            if covered != list(range(1, len(fr.acs) + 1)):
                errors.append(f"{fr.id}: the split parts do not take every acceptance criterion exactly once")
            continue
        prev = out[-1] if out else None
        if (prev and not prev.part and len(prev.frs) == 1 and len(prev.acs) == 1 and len(fr.acs) == 1
                and clean(doc.frs[prev.frs[0]].data.get("screens")) == clean(fr.data.get("screens"))):
            # two tiny FRs of the same screen and section make one story
            prev.frs.append(fr.id)
            prev.acs.append((fr.id, 1, fr.acs[0]))
            prev.summary = f"{prev.summary}; {fr.title[0].lower() + fr.title[1:]}"
            continue
        out.append(Story(fr.brd, [fr.id], [(fr.id, i, a) for i, a in enumerate(fr.acs, start=1)], fr.title, story))
    return out


def story_text(fr: Fr, usv_index: dict, derived: dict[str, str]) -> str:
    rows = usv_index.get(fr.id, [])
    own = [r for b, r in rows if b == fr.brd] or [r for _, r in rows]
    if fr.id in derived:
        return derived[fr.id]
    first = [r for r in own if r["frs"] and r["frs"][0] == fr.id]
    return (first or own)[0]["story"] if own else ""


def number_issues(model: Backlog) -> None:
    for drop in FILE_DROPS:
        n = FIRST_ID[drop]
        for e in model.epics_of(drop):
            e.issue_id = n
            n += 1
        for e in model.epics_of(drop):
            for s in e.stories:
                s.issue_id = n
                n += 1


# ============================================================================================ story content

SUBJECT_START = {"A", "An", "Two", "Three", "Four", "Five", "Six", "Ten", "Each", "Every", "Another"}
PLURAL_START = {"Two", "Three", "Four", "Five", "Six", "Ten"}
RELATIVE = {"that", "which", "who", "whose", "where"}
DETERMINERS = {"a", "an", "the", "of", "with", "for", "its", "their", "each", "every", "and", "or", "to", "in", "on",
               "by", "from", "no", "all", "any", "this", "these", "those", "two", "three", "four", "five", "per",
               "at", "into", "his", "her", "whose", "same", "new", "open", "own"}
VERBS_SINGULAR = set("""is was cannot can does has gets shows lists moves appears receives sees returns stays remains
becomes goes reaches loads posts creates produces generates sends keeps stops starts opens closes carries takes gives
holds needs requires includes contains uses works refuses rejects applies clears changes sets turns leaves matches
fails passes counts reduces increases triggers notifies records marks links adds removes offers allows blocks raises
issues prints exports downloads writes reads computes routes enters joins follows arrives expires locks unlocks saves
sorts filters selects displays prompts asks flags tags assigns approves releases settles books bills pays credits
debits reverses reconciles splits replaces updates resets restores lands falls equals ends runs stores logs finds
excludes must may will should shares""".split())
VERBS_PLURAL = set("""are were have get show appear move receive see return stay remain become go reach load post
create produce generate send keep stop start open close carry take give hold need require include contain use work
refuse reject apply clear change turn leave match fail pass count reduce increase trigger notify record mark link add
remove offer allow block raise issue print export download write read compute route enter join follow arrive expire
lock unlock save sort filter select display prompt ask flag tag assign approve release settle book bill pay credit
debit reverse reconcile split replace update reset restore land fall equal end run store log find exclude share""".split())
CLAUSE_WORDS = {"when", "until", "after", "before", "if", "while", "once", "then", "but"}
PERSON_WORDS = {"user", "users", "officer", "ao", "tl", "th", "head", "lead", "leader", "administrator", "admin",
                "approver", "requester", "requestor", "cashier", "processor", "agent", "handler", "checker", "maker",
                "manager", "supervisor", "investigator", "reviewer", "assistant", "authoriser", "approvers",
                "csr", "member", "members", "operator", "owner", "tester", "employee", "auditor"}
LOWER_FIRST = {"The", "A", "An", "Each", "Every", "No", "All", "Any", "Its", "Their", "This", "These", "Those", "Two",
               "Three", "Four", "Five", "Six", "Ten", "Only", "One", "Both", "Another", "Other", "Some", "On", "After",
               "Before", "Once", "When", "If", "While", "At", "In", "For", "With", "Until", "Within", "Where", "None",
               "Every", "It", "They", "Nothing", "Then", "Users", "User"}


def lower_first(text: str) -> str:
    text = text.strip()
    first = text.split(" ", 1)[0]
    if first in LOWER_FIRST or (first.endswith("ing") and first[:1].isupper() and first[1:].islower()):
        return text[0].lower() + text[1:]
    return text


def no_stop(text: str) -> str:
    return text.strip().rstrip(".").strip()


def split_subject(ac: str) -> tuple[str, str, bool] | None:
    """'A booked invoice that is unpaid shows the indicator' -> ('A booked invoice that is unpaid', 'shows the
    indicator', plural=False); None when the sentence has no clear subject and main verb."""
    words = no_stop(ac).split()
    if len(words) < 3 or words[0] not in SUBJECT_START:
        return None
    relative = False
    depth = 0
    for i, word in enumerate(words[1:], start=1):
        depth += word.count("(") - word.count(")")
        if depth > 0 or word.endswith(")"):
            continue
        bare = word.strip(",;:\"'").lower()
        if bare in RELATIVE:
            relative = True
            continue
        plural_subject = words[0] in PLURAL_START
        if bare in (VERBS_PLURAL if plural_subject else VERBS_SINGULAR):
            if words[i - 1].lower().strip(",") in DETERMINERS:
                continue
            if relative:
                relative = False
                continue
            subject = " ".join(words[:i])
            if i > 14 or ", " in subject or ";" in subject or any(w.lower() in CLAUSE_WORDS for w in words[1:i]) or any(w.lower().strip(",") in PERSON_WORDS
                                                                     for w in words[:i]):
                return None
            return subject, " ".join(words[i:]), plural_subject
    return None


def given_when_then(fr: Fr, n: int, ac: str) -> str:
    """One numbered acceptance criterion of the FR as Given / When / Then. The AC number is kept in brackets."""
    pre = [clean(p) for p in as_list(fr.data.get("preconditions"))]
    pre = [no_stop(p) for p in pre if p and not p.lower().startswith(("none", "as fr", "as for fr"))]
    actor = clean(fr.data.get("actor"))
    if pre:
        given = " and ".join(lower_first(p) for p in pre)
    elif actor.lower().startswith("system"):
        given = "the step is due in BIBS"
    else:
        given = f"the {re.sub(r'[(].*?[)]', '', actor.split(';')[0]).strip()} is signed in to BIBS"
    flow = [f for f in as_list(fr.data.get("main_flow")) if isinstance(f, str)]
    when = lower_first(no_stop(clean(flow[0]))) if flow else "the user carries out the function"
    text = no_stop(ac)
    then = text
    m = re.match(r"^(When|If)\s+([^,;]+?),\s+(.+)$", text)
    c = re.match(r"^(For|With)\s+([^,;]+?),\s+(.+)$", text)
    s = split_subject(text)
    if m:
        when, then = m.group(2), m.group(3)
    elif c:
        given = f"{given} and {lower_first(c.group(2))}"
        then = c.group(3)
    elif s:
        subject, predicate, plural = s
        given = f"{given} and {lower_first(subject)}"
        then = f"{'they' if plural else 'it'} {predicate}"
    return f"(AC{n}) Given {given}, when {when}, then {lower_first(then)}."


def wiki(text: str) -> str:
    """Text that reads the same in Jira wiki markup and in Markdown: no brackets, braces, pipes or image marks."""
    text = clean(text)
    for a, b in (("[", "("), ("]", ")"), ("{", "("), ("}", ")"), ("|", "/"), ("!", ".")):
        text = text.replace(a, b)
    return text


def range_text(ids: list[str]) -> str:
    return ", ".join(ids)


def ac_span(story: Story) -> str:
    by_fr: dict[str, list[int]] = defaultdict(list)
    for fid, n, _ in story.acs:
        by_fr[fid].append(n)
    out = []
    for fid, ns in by_fr.items():
        ns = sorted(ns)
        if ns == list(range(ns[0], ns[-1] + 1)) and len(ns) > 1:
            out.append(f"{fid} AC{ns[0]}-{ns[-1]}")
        else:
            out.append(f"{fid} " + ", ".join(f"AC{k}" for k in ns))
    return "; ".join(out)


def story_points(story: Story, frs: list[Fr]) -> int:
    rule = CONFIG["points"]
    scale = rule["scale"]
    step = rule["base"].get(min(len(story.acs), 6), 4)
    screens = max(len([x for x in clean(fr.data.get("screens")).split(";") if x.strip()]) for fr in frs)
    if screens >= rule["screens_step"]:
        step += 1
    text = " ".join(clean(fr.data.get(k)) for fr in frs
                    for k in ("description", "main_flow", "alternate_flows", "rules", "notifications", "screens"))
    if any(re.search(rf"(?<![A-Za-z]){re.escape(sysname)}(?![A-Za-z])", text) for sysname in rule["systems"]):
        step += 1
    return scale[min(step, len(scale) - 1)]


def complete_story(model: Backlog, s: Story) -> None:
    doc = model.docs[s.brd]
    frs = [doc.frs[f] for f in s.frs]
    fr0 = frs[0]
    e = s.epic
    sv = (CONFIG.get("story_versions") or {}).get(fr0.id)
    s.version, s.version_note = (sv["version"], sv["reason"]) if sv else (e.version, "")
    s.priority = max((priority_of(clean(fr.data.get("priority"))) for fr in frs),
                     key=["Low", "Medium", "High", "Highest"].index)
    s.points = story_points(s, frs)
    s.labels = [s.brd, s.version.replace(" ", "-")] + [p for p in persona_labels(clean(fr0.data.get("actor")))]
    # BRD IDs: the rows of every user-story view that name the FR, then the FR's own references
    ids: list[str] = []
    for fid in s.frs:
        for _, row in model.usv_index.get(fid, []):
            if row["brd_id"] not in ids:
                ids.append(row["brd_id"])
    if not ids:
        ids = [clean(r) for fr in frs for r in as_list(fr.data.get("brd"))]
    s.brd_ids = ids
    # test conditions of the FR (all conditions of a split FR, which the test plan does not divide by AC)
    parts = []
    total = 0
    for fid in s.frs:
        conds = doc.conditions.get(fid, [])
        parts += [c for c, _ in conds]
        total += sum(n for _, n in conds)
    if parts:
        s.tcs = f"{', '.join(parts)} ({len(parts)} conditions, {total} cases)"
    else:
        via: list[str] = []
        for fid in s.frs:
            for _, row in model.usv_index.get(fid, []):
                t = clean(row["tcs"])
                if t and t != "-" and t not in via:
                    via.append(t)
        s.tcs = ("No test plan of its own (umbrella FRS); covered by the cases of the function test plans: "
                 + "; ".join(via[:6])) if via else "No test plan of its own (umbrella FRS)"
    s.frs_ref = f"{doc.label} ({doc.docname}), section {fr0.number} {fr0.section}, " + ", ".join(
        f"{fr.id} {fr.title}" for fr in frs)
    s.gwt = [given_when_then(doc.frs[fid], n, text) for fid, n, text in s.acs]
    # business rules and checks of the FR
    rules: list[str] = []
    for fr in frs:
        for r in as_list(fr.data.get("rules")):
            if isinstance(r, list) and len(r) >= 2:
                kind = f" ({clean(r[2])})" if len(r) > 2 and clean(r[2]) in ("Configurable", "Fixed") else ""
                rules.append(f"({clean(r[0])}) {clean(r[1])}{kind}")
            elif r:
                rules.append(clean(r))
        for v in as_list(fr.data.get("validations")):
            if isinstance(v, list) and len(v) >= 2:
                rules.append(f"Check: {clean(v[0])}. Message: \"{clean(v[1])}\"")
    s.rules = rules
    # notes: clarifications of the FR, release, split
    notes: list[str] = []
    for c in doc.clr:
        if c["frs"] & set(s.frs):
            notes.append(f"{c['id']} - open, for BDOI confirmation: {c['topic']}. Proposed: {c['proposed']} "
                         f"Decision requested: {c['decision']}")
    if model.docs["BRD-00"] is not doc:
        for c in model.docs["BRD-00"].clr:
            if c["frs"] & set(s.frs):
                notes.append(f"{c['id']} (umbrella FRS) - open, for BDOI confirmation: {c['topic']}")
    if s.version_note:
        notes.append(f"Fix version {s.version}: {s.version_note}.")
    if e.version == "Phase 2":
        notes.append(f"Fix version Phase 2: {e.reason}.")
    if s.part:
        others = [o.summary for o in e.stories if o.frs == s.frs and o is not s]
        notes.append(f"Part {s.part} of {fr0.id}; the other part: {'; '.join(others)}.")
    if len(s.frs) > 1:
        notes.append(f"{' and '.join(s.frs)} are one story: each has one acceptance criterion on the same screen.")
    s.notes = notes
    # links: FRS section, screens, walkthrough steps (storyboard), figures, UX deck slides
    links = [f"FRS section {fr0.number} {fr0.section} ({doc.docname})",
             "Screens: " + "; ".join(dict.fromkeys(clean(fr.data.get("screens")) for fr in frs))]
    frames = [r for r in doc.storyboard if set(r["frs"]) & set(s.frs)]
    for r in frames[:8]:
        bits = [r["screen"]] + ([f"figure {r['figure']}"] if r["figure"] and r["figure"] != "-" else []) + (
            [f"UX deck slide {r['slide']}"] if r["slide"] and r["slide"] != "-" else [])
        links.append(f"Walkthrough step {r['frame']} ({r['persona']}): " + ", ".join(b for b in bits if b))
    if len(frames) > 8:
        links.append(f"... and {len(frames) - 8} more walkthrough steps in the storyboard index")
    s.links = links
    refs = [clean(r) for fr in frs for r in as_list(fr.data.get("brd"))]
    s.background = [f"BRD references and pages: {'; '.join(refs)}",
                    f"FRS: {s.frs_ref}",
                    f"Persona: {clean(fr0.data.get('actor'))}"]
    desc = [clean(x) for x in as_list(fr0.data.get("description"))]
    if desc:
        s.background.append(f"Function: {desc[0]}")
    s.description = story_description(s)


def story_description(s: Story) -> str:
    def block(title: str, items: list[str]) -> list[str]:
        return [f"*{title}*"] + [f"* {wiki(i)}" for i in items] + [""]
    lines = [wiki(s.story), ""]
    lines += block("Background", s.background)
    lines += block("Business rules", s.rules or ["As the FR states; no separate rule."])
    lines += block("Acceptance criteria", s.gwt)
    lines += block("Out of scope / notes", s.notes or ["No open clarification on this FR."])
    lines += block("Links", s.links + [f"Test cases: {s.tcs}"])
    return "\n".join(lines).strip()


def epic_description(model: Backlog, e: Epic) -> str:
    doc = model.docs[e.brd]
    frs = [f for s in e.stories for f in s.frs]
    lines = [wiki(f"Capability {e.capability} of {brand.BRD_NAMES.get(e.brd, 'Core Replacement')} ({e.brd}): "
                  f"section {e.number} of the {doc.label} ({doc.docname})."), "",
             wiki(f"Stories: {len(e.stories)}, story points (first estimate): {e.points}. FRs: {range_text(frs)}."),
             "", wiki(f"Fix version: {e.version}." + (f" {e.reason}." if e.reason else ""))]
    return "\n".join(lines)


# ============================================================================================ coverage

def coverage(model: Backlog) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Per BRD: BRD IDs of the user-story view and FRs of the FRS against the stories of each file; and the trace
    of every BRD ID (its stories or the reason it has none)."""
    story_of_fr: dict[str, list[Story]] = defaultdict(list)
    for s in model.stories():
        for f in s.frs:
            story_of_fr[f].append(s)
    summary: list[dict[str, Any]] = []
    trace: list[dict[str, Any]] = []
    for brd in BRDS:
        d = model.docs[brd]
        row: dict[str, Any] = {"brd": brd, "module": brand.BRD_NAMES.get(brd, "Core Replacement (umbrella)"),
                               "ids": 0, "in_story": 0, "no_story": 0, "gap": 0, "frs": len(d.frs),
                               "fr_gap": 0}
        for drop in FILE_DROPS:
            row[f"ids_{drop}"] = 0
            row[f"frs_{drop}"] = 0
        seen: set[str] = set()
        for r in d.usv:
            if r["brd_id"] in seen:
                continue
            seen.add(r["brd_id"])
            row["ids"] += 1
            stories = [s for f in r["frs"] for s in story_of_fr.get(f, [])]
            files = sorted({s.epic.file_drop for s in stories})
            for drop in files:
                row[f"ids_{drop}"] += 1
            if stories:
                row["in_story"] += 1
                status = "In a story"
                reason = ""
            elif r["frs"]:
                status, reason = "Gap", "FR without a story"
                row["gap"] += 1
            else:
                status = "No story"
                reason = clean(r["fr_text"]) if clean(r["fr_text"]) not in ("-", "") else r["story"].split(". ")[0]
                row["no_story"] += 1
            trace.append({"brd": brd, "brd_id": r["brd_id"], "frs": ", ".join(r["frs"]) or "-",
                          "stories": ", ".join(dict.fromkeys(str(s.issue_id) for s in stories)),
                          "files": ", ".join(files), "status": status, "reason": reason})
        for f in d.frs:
            files = {s.epic.file_drop for s in story_of_fr.get(f, [])}
            if not files:
                row["fr_gap"] += 1
            for drop in files:
                row[f"frs_{drop}"] += 1
        summary.append(row)
    return summary, trace


# ============================================================================================ Excel workbooks

def summary_rows(model: Backlog, drop: str) -> list[dict[str, Any]]:
    rows = []
    for brd in BRDS:
        mine = [e for e in model.epics if e.brd == brd]
        here = [e for e in mine if e.file_drop == drop]
        st = [s for e in here for s in e.stories]
        row: dict[str, Any] = {"brd": brd, "module": CONFIG["brds"][brd]["component"], "epics": len(here),
                               "stories": len(st), "points": sum(s.points for s in st),
                               "phase2": sum(1 for s in st if s.version == "Phase 2")}
        for v in VERSIONS:
            vs = [s for e in mine for s in e.stories if s.version == v]
            row[f"s_{v}"] = len(vs)
            row[f"p_{v}"] = sum(s.points for s in vs)
        rows.append(row)
    total = {"brd": "Total", "module": "All BRDs"}
    for k in rows[0]:
        if k not in ("brd", "module"):
            total[k] = sum(r[k] for r in rows)
    return rows + [total]


ISSUE_COLUMNS = [
    Column("type", "Issue Type", 9, "Epic (one business capability of a BRD) or Story (one user-facing increment)"),
    Column("id", "Issue ID", 9, "Temporary number that links a story to its epic in the import file; Jira gives "
                                "the issue key on import", kind="number"),
    Column("parent", "Parent", 9, "Issue ID of the epic of the story (empty on an epic)", kind="number"),
    Column("epic", "Epic", 26, "Name of the epic of the story (readable form of Parent)"),
    Column("summary", "Summary", 40, "Epic: <BRD short name> - <capability>; story: the short form of the story"),
    Column("story", "User story", 48, "As a <persona> I need / want <goal> so that <benefit>: the BRD wording where "
                                      "the BRD writes a story, otherwise derived from the FR"),
    Column("priority", "Priority", 9, "From the MoSCoW priority of the FR (Must have = High)",
           values=["Highest", "High", "Medium", "Low"]),
    Column("labels", "Labels", 18, "BRD, drop or phase, personas (one label per line; repeated Labels columns in "
                                   "the import file)"),
    Column("component", "Components", 16, "Module of the BRD"),
    Column("version", "Fix Version", 10, "Drop 0, Drop 1, Drop 2 or Phase 2", values=VERSIONS),
    Column("points", "Story Points", 8, "First estimate on 1, 2, 3, 5, 8, 13 (rule on the sheet Field mapping and "
                                        "in the import guide)", kind="number"),
    Column("brd_ids", "BRD IDs", 22, "BRD requirement IDs met by the story (rows of the user-story views)"),
    Column("fr_ids", "FR IDs", 16, "FRs of the story with the acceptance criteria it takes"),
    Column("tcs", "Test Cases", 24, "Test conditions of the BRD test plan and their number of cases"),
    Column("frs_ref", "FRS Reference", 30, "FRS or handbook, version, file, section and FR titles"),
    Column("acs", "Acceptance Criteria", 60, "Numbered acceptance criteria of the FR as Given / When / Then; the AC "
                                             "number in brackets traces back to the FRS"),
    Column("rules", "Business rules", 45, "Rules and checks of the FR"),
    Column("notes", "Out of scope / notes", 40, "Open clarifications (CLR) with their status, release notes, split"),
    Column("links", "Links", 40, "FRS section, screens, walkthrough steps with figure and UX deck slide"),
    Column("description", "Description (as imported)", 60, "The text of the Jira description: story, background, "
                                                            "business rules, acceptance criteria, notes and links"),
]


def issue_rows(model: Backlog, brd: str, drop: str) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for e in [e for e in model.epics_of(drop) if e.brd == brd]:
        rows.append(epic_row(e))
        rows += [story_row(s) for s in e.stories]
    return rows


def epic_row(e: Epic) -> dict[str, Any]:
    frs = [f for s in e.stories for f in s.frs]
    ids = list(dict.fromkeys(i for s in e.stories for i in s.brd_ids))
    return {"type": "Epic", "id": e.issue_id, "parent": None, "epic": e.name, "summary": e.name, "story": "",
            "priority": max((s.priority for s in e.stories), key=["Low", "Medium", "High", "Highest"].index),
            "labels": "\n".join([e.brd, e.version.replace(" ", "-")]),
            "component": CONFIG["brds"][e.brd]["component"], "version": e.version, "points": None,
            "brd_ids": ", ".join(ids), "fr_ids": ", ".join(frs), "tcs": "",
            "frs_ref": f"Section {e.number} {e.capability}", "acs": "", "rules": "", "notes": e.reason,
            "links": "", "description": e.description}


def story_row(s: Story) -> dict[str, Any]:
    return {"type": "Story", "id": s.issue_id, "parent": s.epic.issue_id, "epic": s.epic.name,
            "summary": s.summary, "story": s.story, "priority": s.priority, "labels": "\n".join(s.labels),
            "component": CONFIG["brds"][s.brd]["component"], "version": s.version, "points": s.points,
            "brd_ids": ", ".join(s.brd_ids), "fr_ids": ac_span(s), "tcs": s.tcs, "frs_ref": s.frs_ref,
            "acs": "\n".join(s.gwt), "rules": "\n".join(s.rules), "notes": "\n".join(s.notes),
            "links": "\n".join(s.links), "description": s.description}


def field_mapping() -> list[dict[str, Any]]:
    rows = [
        ["Issue Type", "Issue Type", "Issue Type", "Issue Type", "Standard", "Epic or Story"],
        ["Issue ID", "Issue ID", "Issue Id", "Issue Id", "Import link only", "Temporary number; not kept as a field"],
        ["Parent", "Parent", "Parent", "Parent", "Standard", "Issue ID of the epic in the same file"],
        ["Summary", "Summary", "Summary", "Summary", "Standard", "Up to 255 characters"],
        ["Epic (name)", "Epic Name", "Epic Name (when the project still shows it)", "Do not map", "Standard",
         "Filled on epic rows only"],
        ["Description (as imported)", "Description", "Description", "Description", "Standard",
         "Story, background, business rules, acceptance criteria, notes, links"],
        ["Priority", "Priority", "Priority", "Priority", "Standard", "Highest, High, Medium, Low"],
        ["Labels", "Labels (repeated columns)", "Labels", "Labels", "Standard", "One value per Labels column"],
        ["Components", "Component", "Component/s", "Labels (team-managed projects have no components)",
         "Standard", "One module per issue"],
        ["Fix Version", "Fix Version", "Fix Version/s", "Fix versions", "Standard",
         "The versions are created before the import"],
        ["Story Points", "Story Points", "Story Points", "Story point estimate", "Number", "Stories only"],
        ["BRD IDs", "BRD IDs", "BRD IDs (custom)", "BRD IDs (custom)", "Paragraph (multi-line text)", ""],
        ["FR IDs", "FR IDs", "FR IDs (custom)", "FR IDs (custom)", "Paragraph (multi-line text)", ""],
        ["Test Cases", "Test Cases", "Test Cases (custom)", "Test Cases (custom)", "Paragraph (multi-line text)", ""],
        ["FRS Reference", "FRS Reference", "FRS Reference (custom)", "FRS Reference (custom)",
         "Paragraph (multi-line text)", ""],
        ["Acceptance Criteria", "Acceptance Criteria", "Acceptance Criteria (custom)",
         "Acceptance Criteria (custom)", "Paragraph (multi-line text)", "Also in the description"],
        ["User story, Business rules, Out of scope / notes, Links", "-", "Not imported as fields", "-", "-",
         "Readable columns of this workbook; the same text is in the description"],
    ]
    keys = ["workbook", "csv", "company", "team", "type", "notes"]
    return [dict(zip(keys, r)) for r in rows]


def build_workbook(model: Backlog, drop: str, cov: tuple[list, list]) -> Path:
    code = brand.drop_code(drop)
    wb = BdoiWorkbook(f"User Story Backlog {drop}", doc_type="User story backlog", brd="BRD-00", version=VERSION,
                      date=DATE, subtitle=f"Epics and stories of {brand.DROPS[drop]['title']} for the BIBS Jira "
                                          "project, generated from the FRS and the test plans")
    wb.cover_notes = [
        "Generated from the FRS sources (functional requirements with their numbered acceptance criteria, user-story "
        "view, storyboard index, clarifications) and the test plans of each BRD; the CSV import file of the same "
        f"drop, BIBS_Jira_Import_{code}.csv, holds the same issues.",
        "No status column: every issue imports with the initial status of the project workflow.",
    ]
    sm_cols = [Column("brd", "BRD", 8), Column("module", "Module", 24),
               Column("epics", "Epics in this file", 9, kind="number"),
               Column("stories", "Stories in this file", 9, kind="number"),
               Column("points", "Story points in this file", 10, kind="number"),
               Column("phase2", "of which Phase 2 stories", 9, "Stories of this file with the fix version Phase 2",
                      kind="number")]
    for v in VERSIONS:
        sm_cols += [Column(f"s_{v}", f"Stories {v}", 9, f"Stories of the BRD with the fix version {v} (all files)",
                           kind="number"),
                    Column(f"p_{v}", f"Points {v}", 9, f"Story points of the BRD with the fix version {v}",
                           kind="number")]
    wb.sheet("Summary", sm_cols, summary_rows(model, drop),
             description=f"Epics, stories and story points per BRD in this file ({drop}) and per fix version of the "
                         "whole backlog (first estimate)")
    for brd in BRDS:
        rows = issue_rows(model, brd, drop)
        if rows:
            name = f"{brd} {CONFIG['brds'][brd]['short']}"[:31]
            wb.sheet(name, ISSUE_COLUMNS, rows,
                     description=f"{brand.BRD_NAMES.get(brd, 'Core Replacement')}: each epic row, then its stories")
    summary, trace = cov
    cov_cols = [Column("brd", "BRD", 8), Column("module", "Module", 24),
                Column("ids", "BRD IDs in the user-story view", 11, "Distinct BRD IDs of the FRS appendix",
                       kind="number"),
                Column("in_story", "BRD IDs in at least one story", 11, "In a story of any of the three files",
                       kind="number"),
                Column(f"ids_{drop}", "BRD IDs with a story in this file", 11, kind="number")]
    cov_cols += [Column(f"ids_{d}", f"BRD IDs with a story in {d}", 10, kind="number") for d in FILE_DROPS if
                 d != drop]
    cov_cols += [Column("no_story", "BRD IDs without a story (reason on BRD ID trace)", 12,
                        "Phase 2, removed, moved or out of scope by the BRD, or waiting for a clarification",
                        kind="number"),
                 Column("gap", "Gaps", 7, "BRD IDs whose FR has no story (must be 0)", kind="number"),
                 Column("frs", "FRs in the FRS", 9, kind="number"),
                 Column(f"frs_{drop}", "FRs in stories of this file", 9, kind="number")]
    cov_cols += [Column(f"frs_{d}", f"FRs in stories of {d}", 9, kind="number") for d in FILE_DROPS if d != drop]
    cov_cols += [Column("fr_gap", "FRs without a story", 9, "Must be 0", kind="number"),
                 Column("result", "Result", 28, "Every BRD ID and every FR is in at least one story, or the BRD ID "
                                                "has no FR for the reason given")]
    rows = []
    for r in summary:
        rr = dict(r)
        rr["result"] = ("Complete" if not r["gap"] and not r["fr_gap"]
                        else f"Gap: {r['gap']} BRD IDs, {r['fr_gap']} FRs")
        rows.append(rr)
    tot = {"brd": "Total", "module": "All BRDs", "result": ""}
    for k in rows[0]:
        if k not in tot:
            tot[k] = sum(r[k] for r in rows)
    tot["result"] = "Complete" if not tot["gap"] and not tot["fr_gap"] else "Gaps listed on BRD ID trace"
    wb.sheet("Coverage", cov_cols, rows + [tot],
             description="Every BRD ID of the user-story views and every FR of the FRS against the stories of the "
                         "three files; the BRD ID trace sheet lists each BRD ID")
    brds_here = {e.brd for e in model.epics_of(drop)}
    tr_cols = [Column("brd", "BRD", 8), Column("brd_id", "BRD ID", 22), Column("frs", "FR IDs", 26),
               Column("stories", "Stories (Issue ID)", 22, "Issue IDs of the stories that meet the BRD ID"),
               Column("files", "Import files", 14, "Drop of the files that hold the stories"),
               Column("status", "Coverage", 11, values=["In a story", "No story", "Gap"]),
               Column("reason", "Reason (no story)", 50)]
    wb.sheet("BRD ID trace", tr_cols, [t for t in trace if t["brd"] in brds_here],
             description="One row per BRD ID of the BRDs of this file: the stories that meet it, or why it has none")
    dod = [{"list": "Definition of Ready", "item": a, "text": b} for a, b in CONFIG["ready"]]
    dod += [{"list": "Definition of Done", "item": a, "text": b} for a, b in CONFIG["done"]]
    wb.sheet("Ready and Done", [Column("list", "List", 20), Column("item", "Item", 22),
                                Column("text", "Criterion", 100)], dod,
             description="Definition of Ready and Definition of Done of a BIBS story, consistent with the Test "
                         "Strategy (traceability, test plans, severity levels, SIT and UAT exit)")
    wb.sheet("Field mapping", [Column("workbook", "Workbook column", 24), Column("csv", "Import file column", 20),
                               Column("company", "Jira field (company-managed)", 28),
                               Column("team", "Jira field (team-managed)", 28), Column("type", "Field type", 22),
                               Column("notes", "Notes", 44)], field_mapping(),
             description="Each column of the BRD sheets and of the import file and the Jira field it is mapped to")
    path = OUT / brand.output_name("Backlog", "BRD-00", f"User Story Backlog {code}", VERSION, "xlsx")
    return wb.save(path)


# ============================================================================================ CSV import files

CSV_FIELDS = ["Issue Type", "Issue ID", "Parent", "Summary", "Epic Name", "Description", "Priority"]
CSV_TAIL = ["Component", "Fix Version", "Story Points", "BRD IDs", "FR IDs", "Test Cases", "FRS Reference",
            "Acceptance Criteria"]


def label_columns(model: Backlog) -> int:
    return max(len(s.labels) for s in model.stories())


def csv_path(drop: str) -> Path:
    return JIRA_DIR / f"BIBS_Jira_Import_{brand.drop_code(drop)}.csv"


def write_csv(model: Backlog, drop: str) -> Path:
    n = label_columns(model)
    path = csv_path(drop)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(CSV_FIELDS + ["Labels"] * n + CSV_TAIL)
        epics = model.epics_of(drop)
        for e in epics:
            r = epic_row(e)
            labels = r["labels"].split("\n")
            w.writerow(["Epic", e.issue_id, "", e.name, e.name, e.description, r["priority"]]
                       + labels + [""] * (n - len(labels))
                       + [r["component"], e.version, "", r["brd_ids"], r["fr_ids"], "", r["frs_ref"], ""])
        for e in epics:
            for s in e.stories:
                w.writerow(["Story", s.issue_id, e.issue_id, s.summary, "", s.description, s.priority]
                           + s.labels + [""] * (n - len(s.labels))
                           + [CONFIG["brds"][s.brd]["component"], s.version, s.points, ", ".join(s.brd_ids),
                              ac_span(s), s.tcs, s.frs_ref, "\n".join(s.gwt)])
    return path


def check_csv(path: Path) -> tuple[list[str], dict[str, int]]:
    """Re-reads an import file: one header row, unique Issue IDs, every Parent an epic of the file, every story with
    a parent epic, labels without spaces, story points on the scale."""
    errors: list[str] = []
    with path.open(encoding="utf-8", newline="") as fh:
        rows = list(csv.reader(fh))
    header, body = rows[0], rows[1:]
    col = {h: i for i, h in reversed(list(enumerate(header)))}
    label_idx = [i for i, h in enumerate(header) if h == "Labels"]
    ids = [r[col["Issue ID"]] for r in body]
    if len(ids) != len(set(ids)):
        errors.append(f"{path.name}: Issue IDs are not unique")
    types = {r[col["Issue ID"]]: r[col["Issue Type"]] for r in body}
    counts = Counter()
    points = 0
    for r in body:
        if len(r) != len(header):
            errors.append(f"{path.name} {r[col['Issue ID']]}: {len(r)} cells for {len(header)} columns")
        kind = r[col["Issue Type"]]
        counts[kind] += 1
        parent = r[col["Parent"]]
        if kind == "Story":
            if not parent:
                errors.append(f"{path.name} {r[col['Issue ID']]}: story without a parent epic")
            elif types.get(parent) != "Epic":
                errors.append(f"{path.name} {r[col['Issue ID']]}: parent {parent} is not an epic of the file")
            if r[col["Story Points"]] not in {str(x) for x in CONFIG["points"]["scale"]}:
                errors.append(f"{path.name} {r[col['Issue ID']]}: story points {r[col['Story Points']]}")
            points += int(r[col["Story Points"]] or 0)
            counts["Phase 2"] += r[col["Fix Version"]] == "Phase 2"
        elif kind == "Epic":
            if parent:
                errors.append(f"{path.name} {r[col['Issue ID']]}: an epic has a parent")
        else:
            errors.append(f"{path.name} {r[col['Issue ID']]}: issue type {kind}")
        for i in label_idx:
            if " " in r[i]:
                errors.append(f"{path.name} {r[col['Issue ID']]}: label with a space: {r[i]}")
        if len(r[col["Summary"]]) > 255:
            errors.append(f"{path.name} {r[col['Issue ID']]}: summary over 255 characters")
        for name in ("Description", "Acceptance Criteria", "BRD IDs", "Test Cases", "FRS Reference"):
            if len(r[col[name]]) > 32000:
                errors.append(f"{path.name} {r[col['Issue ID']]}: {name} over 32,000 characters")
    counts["points"] = points
    return errors, dict(counts)


# ============================================================================================ Word guide

def totals(model: Backlog) -> dict[str, Any]:
    t: dict[str, Any] = {"epics": len(model.epics), "stories": len(model.stories()),
                         "points": sum(s.points for s in model.stories()),
                         "frs": sum(len(d.frs) for d in model.docs.values()),
                         "splits": len(CONFIG.get("splits") or {}),
                         "split_stories": sum(len(p) for p in (CONFIG.get("splits") or {}).values()),
                         "merged": sum(1 for s in model.stories() if len(s.frs) > 1),
                         "labels": label_columns(model), "version": VERSION, "date": DATE}
    for drop in FILE_DROPS:
        k = brand.drop_code(drop).replace("-", "").lower()
        st = model.stories(drop)
        t[f"{k}_epics"] = len(model.epics_of(drop))
        t[f"{k}_stories"] = len(st)
        t[f"{k}_points"] = sum(s.points for s in st)
        t[f"{k}_issues"] = len(st) + len(model.epics_of(drop))
    cov, _ = coverage(model)
    t["brd_ids"] = sum(r["ids"] for r in cov)
    t["no_story"] = sum(r["no_story"] for r in cov)
    t["gaps"] = sum(r["gap"] for r in cov) + sum(r["fr_gap"] for r in cov)
    t["in_story"] = t["brd_ids"] - t["no_story"] - sum(r["gap"] for r in cov)
    return t


def cell(value: Any) -> str:
    return str(value if value is not None else "").replace("|", "/").replace("\n", " ").strip()


def table(headers: list[str], rows: list[list[Any]], widths: str, caption: str, size: str = "8.5") -> list[str]:
    out = [f'<!-- table: widths={widths} caption="{caption}" size={size} -->', "| " + " | ".join(headers) + " |",
           "|" + "---|" * len(headers)]
    return out + ["| " + " | ".join(cell(c) for c in r) + " |" for r in rows] + [""]


def render(model: Backlog, name: str) -> list[str]:
    t = totals(model)
    if name == "files":
        rows = []
        for drop in FILE_DROPS:
            code = brand.drop_code(drop)
            rows.append([drop, brand.output_name("Backlog", "BRD-00", f"User Story Backlog {code}", VERSION, "xlsx"),
                         f"BIBS_Jira_Import_{code}.csv", t[f"{code.replace('-', '').lower()}_issues"]])
        return table(["Drop", "Workbook", "Import file (folder jira)", "Issues"], rows, "1.6,7.4,5,1.4",
                     "The backlog files")
    if name == "counts":
        rows = []
        for drop in FILE_DROPS:
            st = model.stories(drop)
            p2 = [s for s in st if s.version == "Phase 2"]
            rows.append([drop, len(model.epics_of(drop)), len(st), sum(s.points for s in st), len(p2),
                         len(st) + len(model.epics_of(drop))])
        rows.append(["Total", t["epics"], t["stories"], t["points"],
                     sum(1 for s in model.stories() if s.version == "Phase 2"),
                     t["epics"] + t["stories"]])
        return table(["Import file", "Epics", "Stories", "Story points", "of which Phase 2 stories", "Issues"],
                     rows, "3,1.8,1.8,2,2.6,1.8", "Issues per import file (compare with the Summary sheet)")
    if name == "brds":
        rows = []
        for brd in BRDS:
            ep = [e for e in model.epics if e.brd == brd]
            row: list[Any] = [brd, CONFIG["brds"][brd]["component"], len(ep)]
            for drop in FILE_DROPS:
                st = [s for e in ep if e.file_drop == drop for s in e.stories]
                row.append(f"{len(st)} / {sum(s.points for s in st)}" if st else "-")
            rows.append(row)
        return table(["BRD", "Module (component)", "Epics", "Drop 0 stories / points", "Drop 1 stories / points",
                      "Drop 2 stories / points"], rows, "1.4,4.6,1.2,2.6,2.6,2.6",
                     "Epics, stories and story points per BRD and import file", size="8")
    if name == "drops":
        rows = []
        for brd in BRDS:
            spec = CONFIG["drops"].get(brd, {})
            primary = spec.get("default") if brd == "BRD-00" else brand.BRD_DROP[brd]
            moved = "; ".join(f"{k}: {v}" for k, v in (spec.get("sections") or {}).items()) or "-"
            rows.append([brd, primary, moved])
        rows.append(["BRD-12", "Story FR-SP-002", "Phase 2 (BRD Release 2), in the Drop 1 file"])
        return table(["BRD", "Drop of the epics", "Epics in another drop or phase"], rows, "1.4,3,12.6",
                     "Drop of the epics (fix version)", size="8")
    if name == "components":
        rows = [[CONFIG["brds"][b]["component"], b, brand.BRD_NAMES.get(b, "Core Replacement (umbrella)")]
                for b in BRDS]
        return table(["Component", "BRD", "BRD name"], rows, "5,1.6,7", "Components to create")
    if name == "versions":
        rows = [[d, brand.DROPS[d]["title"], brand.DROPS[d]["dates"]] for d in FILE_DROPS]
        rows.append(["Phase 2", "After the go-live of January 2028", "Stories held for phase 2 (EB partner "
                                                                     "portal; BRD-12 Release 2)"])
        return table(["Fix version", "Name", "Dates (BDOI drop plan of 26-Sep-2026)"], rows, "2,5,10",
                     "Fix versions to create", size="8")
    if name == "fields":
        rows = [["BRD IDs", "Paragraph (multi-line text)", "BRD requirement IDs met by the story"],
                ["FR IDs", "Paragraph (multi-line text)", "FRs and their acceptance criteria numbers"],
                ["Test Cases", "Paragraph (multi-line text)", "Test conditions and number of cases"],
                ["FRS Reference", "Paragraph (multi-line text)", "FRS, version, file, section and FR"],
                ["Acceptance Criteria", "Paragraph (multi-line text)", "Given / When / Then with (ACn)"]]
        return table(["Field name (exact)", "Type", "Holds"], rows, "4,5,8", "Custom fields to create")
    if name == "mapping":
        rows = [[r["csv"], r["company"], r["team"]] for r in field_mapping() if r["csv"] != "-"]
        return table(["Column of the import file", "Map to (company-managed)", "Map to (team-managed)"], rows,
                     "4.6,6,6.4", "Mapping of each column in the importer", size="8")
    if name == "priority":
        rows = [[k, v] for k, v in CONFIG["priority"]]
        return table(["Priority of the FR (MoSCoW or BRD level)", "Jira priority"], rows, "8,4",
                     "Priority mapping")
    if name == "points":
        rows = [["1 acceptance criterion", "1"], ["2", "2"], ["3", "3"], ["4 or 5", "5"], ["6 or more", "8"],
                ["Three screens or more", "one step up the scale"],
                ["Exchange with a BDO or external system", "one step up the scale"], ["Highest value", "13"]]
        return table(["Story", "Story points"], rows, "8,5", "First estimate of the story points")
    if name == "checks":
        rows = []
        for drop in FILE_DROPS:
            code = brand.drop_code(drop)
            st = model.stories(drop)
            rows.append([f"fixVersion and file {drop}", len(model.epics_of(drop)), len(st),
                         sum(s.points for s in st)])
        return table(["After importing", "Epics", "Stories", "Sum of story points"], rows, "6,2,2,3",
                     "Counts to compare after each import")
    if name == "coverage":
        cov, _ = coverage(model)
        rows = [[r["brd"], r["ids"], r["ids"] - r["no_story"] - r["gap"], r["no_story"], r["gap"], r["frs"],
                 r["frs"] - r["fr_gap"]] for r in cov]
        rows.append(["Total", t["brd_ids"], t["in_story"], t["no_story"], sum(r["gap"] for r in cov),
                     t["frs"], t["frs"] - sum(r["fr_gap"] for r in cov)])
        return table(["BRD", "BRD IDs", "In a story", "Without a story", "Gaps", "FRs", "FRs in a story"], rows,
                     "1.6,1.8,1.8,2.2,1.4,1.4,2", "Coverage of the BRD IDs and FRs", size="8")
    if name in ("ready", "done"):
        rows = [[a, b] for a, b in CONFIG[name]]
        caption = "Definition of Ready" if name == "ready" else "Definition of Done"
        return table(["Item", "Criterion"], rows, "3.4,13.6", caption, size="8.5")
    raise KeyError(name)


def build_guide(model: Backlog, pdf: bool) -> Path:
    front, lines = load_source(GUIDE_SRC)
    t = totals(model)
    out: list[str] = []
    for line in lines:
        m = PLACEHOLDER.match(line.strip())
        if m:
            out += render(model, m.group(1))
        else:
            out.append(TOKEN.sub(lambda k: f"{t[k.group(1)]:,}" if isinstance(t[k.group(1)], int)
                                 else str(t[k.group(1)]), line))
    doc = BdoiDocument(meta_from(front), h1_page_break=bool(front.get("h1_page_break", False)), base_dir=HERE)
    doc.cover()
    doc.front_matter()
    render_body(doc, out)
    target = OUT / brand.output_name("Backlog", "BRD-00", "Jira Import Guide", VERSION, "docx")
    return doc.publish(target, pdf=pdf)[0]


# ============================================================================================ main

def check(model: Backlog) -> list[str]:
    errors = list(model.errors)
    for drop in FILE_DROPS:
        path = csv_path(drop)
        if not path.exists():
            errors.append(f"{path.name} missing")
            continue
        errs, counts = check_csv(path)
        errors += errs
        st = model.stories(drop)
        expected = {"Epic": len(model.epics_of(drop)), "Story": len(st), "points": sum(s.points for s in st),
                    "Phase 2": sum(1 for s in st if s.version == "Phase 2")}
        for k, v in expected.items():
            if counts.get(k, 0) != v:
                errors.append(f"{path.name}: {k} {counts.get(k, 0)} in the file, {v} in the backlog")
    cov, trace = coverage(model)
    for r in trace:
        if r["status"] == "Gap":
            errors.append(f"coverage gap: {r['brd']} {r['brd_id']} ({r['frs']})")
    for r in cov:
        if r["fr_gap"]:
            errors.append(f"{r['brd']}: {r['fr_gap']} FRs without a story")
    for e in model.epics:
        if not e.stories:
            errors.append(f"{e.name}: epic without stories")
    return errors


def report(model: Backlog) -> str:
    t = totals(model)
    lines = [f"backlog: {t['epics']} epics, {t['stories']} stories, {t['points']} story points"]
    for drop in FILE_DROPS:
        k = brand.drop_code(drop).replace("-", "").lower()
        lines.append(f"  {drop}: {t[k + '_epics']} epics, {t[k + '_stories']} stories, {t[k + '_points']} points")
    lines.append(f"  BRD IDs {t['brd_ids']}: in a story {t['in_story']}, without a story {t['no_story']}, "
                 f"gaps {t['gaps']}")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--check", action="store_true", help="check the sources and the written import files only")
    ap.add_argument("--no-pdf", action="store_true", help="guide without the page numbers of the contents")
    ap.add_argument("--skip-guide", action="store_true", help="workbooks and import files only")
    args = ap.parse_args(argv)
    model = build_model()
    if not args.check:
        cov = coverage(model)
        for drop in FILE_DROPS:
            print(build_workbook(model, drop, cov))
            print(write_csv(model, drop))
        if not args.skip_guide:
            print(build_guide(model, pdf=not args.no_pdf))
    errors = check(model)
    print(report(model))
    for e in errors:
        print(f"error: {e}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
