"""Builds the BDOI reverse KT deck, floor-walk edition (deliverable 41, v3.0).

The end-to-end deck (build_process_deck.py, v2.0) compares BDOI's processes with the BRDs. This edition plays back
only what the project team saw and heard: the BDOI Day 1 overview and floor-walk slides, the floor-walk notes and
the business process flows BDOI shared. It leaves out the BRD review and build status, and the areas that were not
walked (Reinsurance, Sanction Screening, User Access).

Content: ``process_deck/deck_floorwalk.yaml`` (opening, holistic chapter, parts, cross-cutting, roadmap) and the
``fw:`` block of each ``process_deck/areas/*.yaml`` (chapter number, figures, sources, and where needed an As-Is
swimlane and pain points drawn from BDOI's process flows, what the business wants, best practice). Fields an area
does not override come from the area file, with any BRD, register or requirement reference removed.

    python docs/deliverables/src/decks/build_floorwalk_deck.py
    python docs/deliverables/src/decks/build_floorwalk_deck.py --previews [dir]
"""

from __future__ import annotations

import argparse
import re
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import build_process_deck as bp  # noqa: E402
from build_process_deck import LEVEL, ProcessDeck, brand, load, pf, render  # noqa: E402

VERSION = "3.0"
OUT = brand.out_dir("BRD-00", "Decks") / brand.output_name(
    "Deck", "BRD-00", "Business Process Reverse KT Floor Walk", VERSION, "pptx")

# A reference to the BRDs, the register or a requirement / question ID; floor-walk refs (NB-03, CA-01, OV-03...)
# and process-flow refs are kept.
BRD_REF = re.compile(
    r"BRD|DCR-|FRS|Workshop addendum|Addendum|Report List|\bQ\d|\b[A-Z]{1,5}Q\d|\bBR[A-Z]*[.\-]\d|"
    r"\b[A-Z]{3,6}ID\.\d|PMADD|\bFRID|\bCORE-|\bSNSRP|\bDIS \d|\bACSL \d|\bFRBS \d|\bMKT \d|drop plan")
BEST_NOTE = ("Practices of insurance brokers that fit what we saw at BDOI; each card says what the practice is and "
             "where BDOI could start.")


def clean_refs(ref: str) -> str:
    """Drops the BRD / register parts of a '; '-separated reference list."""
    parts = [p.strip() for p in ref.split(";")]
    return "; ".join(p for p in parts if p and not BRD_REF.search(p))


def clean_text(text: str) -> str:
    """Removes parenthesised references to the BRDs or the register from a sentence."""
    def fix(m: re.Match) -> str:
        inner = clean_refs(m.group(1).replace(", ", "; "))
        return f" ({inner.replace('; ', ', ')})" if inner else ""
    return re.sub(r"\s*\(([^()]*)\)", lambda m: fix(m) if BRD_REF.search(m.group(1)) else m.group(0), text)


def merge(a: dict) -> dict:
    """The area as the floor-walk edition shows it: the fw overrides, and every inherited text cleaned."""
    fw = a["fw"]
    m = dict(a)
    m["_source"] = a
    for key in ("num", "title", "short", "roles", "figures", "sources", "session", "wants", "best", "pains",
                "variants"):
        if key in fw:
            m[key] = fw[key]
    m["notes"] = {k: clean_text(v) for k, v in {**a.get("notes", {}), **fw.get("notes", {})}.items()
                  if isinstance(v, str)}
    m["notes"]["best"] = fw.get("notes", {}).get("best", BEST_NOTE)
    if "asis" in fw:
        m["asis"], m["asis_extra"] = fw["asis"], []
    else:
        m["asis"] = {**a["asis"], "caption": fw.get("asis_caption") or clean_text(a["asis"]["caption"]),
                     "notes": clean_text(a["asis"]["notes"])}
        m["asis_extra"] = [{**x, "caption": clean_text(x["caption"]), "notes": clean_text(x["notes"])}
                           for x in a.get("asis_extra") or []]
    if "pains" not in fw:
        refs = fw.get("pains_refs", {})
        m["pains"] = [{**p, "ref": refs.get(p["n"]) or clean_refs(p["ref"]) or "Floor walk"} for p in a["pains"]]
    if "best" not in fw:
        m["best"] = [{**b, "what": clean_text(b["what"]), "rec": clean_text(b["rec"])} for b in a["best"]]
    m["heard"] = [{**h, "asis": clean_text(h["asis"]), **({"want": clean_text(h["want"])} if h.get("want") else {})}
                  for h in a["heard"]]
    if m.get("variants") and "variants" not in fw:
        v = m["variants"]
        m["variants"] = {**v, "notes": fw.get("variants_note") or clean_text(v.get("notes", ""))}
    return m


def validate(a: dict) -> list[str]:
    name = a["key"]
    errs = []
    pains = {p["n"] for p in a["pains"]}
    diagrams = [("asis", a["asis"])] + [(f"asis_{k + 2}", x) for k, x in enumerate(a["asis_extra"])]
    for d, spec in diagrams:
        errs += bp._check_swimlane(name, d, spec, pains)
    marked = {p for _, spec in diagrams for s in spec["steps"] for p in (s.get("pain") or [])}
    errs += [f"{name}: pain point {p} is not marked on an As-Is diagram" for p in sorted(pains - marked)]
    errs += [f"{name}: wants row {r['n']} has no pain point" for r in a["wants"] if r["n"] not in pains]
    errs += [f"{name}: pain point {p} has no wants row" for p in sorted(pains - {r["n"] for r in a["wants"]})]
    for b in a["best"]:
        if not all(isinstance(b.get(k), str) for k in ("practice", "what", "rec")):
            errs.append(f"{name}: best practice {b.get('practice')} is incomplete")
    return errs


def load_areas(keys: list[str]) -> dict[str, dict]:
    areas, errs = {}, []
    for p in sorted((bp.DATA / "areas").glob("*.yaml")):
        a = load(p)
        if a["key"] not in keys:
            continue
        if "fw" not in a:
            errs.append(f"{p.name}: no fw block")
            continue
        m = merge(a)
        errs += validate(m)
        areas[m["key"]] = m
    missing = set(keys) - set(areas)
    errs += [f"area {k} not found" for k in sorted(missing)]
    if errs:
        raise SystemExit("Content errors:\n  " + "\n  ".join(errs))
    return areas


def area_figures(a: dict) -> dict[str, Path]:
    """As-Is figures: an area with its own floor-walk swimlane gets its own files; the others use the end-to-end
    deck's figures as they are."""
    if "asis" not in a["fw"]:
        return bp.area_figures(a["_source"])
    base = f"{a['id']}_fw"
    out = {"asis": render(f"{base}_asis", pf.swimlane(f"{base}_asis", a["asis"], "asis",
                                                      f"{a['title']}: As-Is swimlane. {a['asis']['caption']}"))}
    for k, extra in enumerate(a["asis_extra"]):
        name = f"{base}_asis_{k + 2}"
        out[f"asis_{k + 2}"] = render(name, pf.swimlane(name, extra, "asis",
                                                        f"{a['title']}: As-Is swimlane. {extra['caption']}"))
    return out


def shared(png: Path, twin: str) -> Path:
    """The end-to-end deck's figure ``twin`` when this edition rendered the same image (the pack refuses
    duplicate files); otherwise this edition's own figure."""
    other = bp.FIG / f"{twin}.png"
    if other.exists() and other.read_bytes() == png.read_bytes():
        png.unlink()
        png.with_suffix(".dot").unlink()
        return other
    return png


def holistic_figures(h: dict, areas: dict[str, dict]) -> dict[str, Path]:
    out = {"asis_map": shared(render("holistic_fw_asis_map", pf.free_layout(
        "holistic_fw_asis_map", bp.asis_map_spec(h["asis_map"], areas), h["asis_map"]["caption"])),
        "holistic_asis_map")}
    for key in ("e2e_1", "e2e_2"):
        name = f"holistic_fw_{key}"
        out[key] = shared(render(name, pf.swimlane(name, h[key], "e2e", h[key]["caption"])), f"holistic_{key}")
    return out


def chapter(deck: ProcessDeck, a: dict, figs: dict[str, Path]) -> None:
    deck.glance(a)
    deck.heard(a)
    n = 1 + len(a["asis_extra"])
    deck.diagram(f"{a['num']}. {bp._short(a)}: As-Is process" + (f" (1/{n})" if n > 1 else ""), figs["asis"],
                 a["asis"]["caption"], a["asis"]["notes"])
    for k, extra in enumerate(a["asis_extra"]):
        deck.diagram(f"{a['num']}. {bp._short(a)}: As-Is process ({k + 2}/{n})", figs[f"asis_{k + 2}"],
                     extra["caption"], extra["notes"])
    # a table of segments or routes follows the As-Is; BDOI's own To-Be flow follows what the business wants
    after_wants = a["fw"].get("variants_place") == "wants"

    def variants() -> None:
        if a.get("variants"):
            v = a["variants"]
            deck.table_slide(f"{a['num']}. {bp._short(a)}: {v['title']}", v, per_page=v.get("per_page", 6))

    if not after_wants:
        variants()
    deck.pain_legend(a)
    deck.before_after(a, rows_key="wants", suffix="what the business wants",
                      heads=("Pain point today", "What the business wants", "Lever"))
    if after_wants:
        variants()
    deck.best_practice(a, chips=False)


def build(previews: str | None = None) -> Path:
    d = load(bp.DATA / "deck_floorwalk.yaml")
    keys = [k for p in d["parts"] for k in p["areas"]]
    areas = load_areas(keys)
    hol = holistic_figures(d["holistic"], areas)
    figs = {k: area_figures(a) for k, a in areas.items()}

    deck = ProcessDeck(d["title"], version=VERSION, date=d["date"], subtitle=d["subtitle"])
    deck.title()
    deck.notes(d["notes"]["title"])
    u = d["understanding"]
    deck.cards(u["purpose"]["title"], u["purpose"]["cards"], cols=3, notes=u["purpose"]["notes"],
               intro=u["purpose"]["intro"], size=15, head_size=15)
    deck.cards(u["summary"]["title"], u["summary"]["cards"], cols=3, notes=u["summary"]["notes"], size=14)
    deck.stats(u["sources"]["title"], u["sources"]["tiles"], u["sources"]["notes"], foot=u["sources"]["foot"])
    deck.table_slide(u["schedule"]["title"], u["schedule"], per_page=9)
    tiles = [{"num": "0", "title": "Our understanding of BDOI, end to end", "sub": "Holistic view",
              "highlight": True}]
    for p in d["parts"]:
        for k in p["areas"]:
            tiles.append({"num": str(areas[k]["num"]), "title": areas[k]["title"], "sub": f"Part {p['num']}"})
    tiles += [{**t, "highlight": True} for t in d["closing_tiles"]]
    deck.agenda("Agenda", tiles)
    deck.notes(d["notes"]["agenda"])
    deck.content("How to read this deck", d["reading"], size=16)
    deck.notes(d["notes"]["reading"])

    h = d["holistic"]
    deck.section("0. Our understanding of BDOI", h["subtitle"])
    deck.notes(h["notes"])
    deck.stats(h["glance"]["title"], h["glance"]["tiles"], h["glance"]["notes"], foot=h["glance"].get("foot", ""))
    deck.cards(h["org"]["title"], h["org"]["cards"], cols=4, notes=h["org"]["notes"], size=11, head_size=12)
    deck.lifecycle(h["lifecycle"]["title"], h["lifecycle"])
    for key in ("e2e_1", "e2e_2", "asis_map"):
        deck.diagram(h[key]["title"], hol[key], h[key]["caption"], h[key]["notes"])
    deck.refs_figure(h["refs"]["title"], h["refs"])
    deck.table_slide(h["handoffs"]["title"], h["handoffs"], per_page=8)
    deck.cards(h["challenges"]["title"], h["challenges"]["cards"], cols=2, notes=h["challenges"]["notes"], size=14)
    deck.table_slide(h["aspirations"]["title"], h["aspirations"], per_page=9)

    for p in d["parts"]:
        deck.part({**p, "chapters": [{"num": areas[k]["num"], "title": areas[k]["title"],
                                      "sub": areas[k]["department"]} for k in p["areas"]]})
        for k in p["areas"]:
            chapter(deck, areas[k], figs[k])

    x = d["crosscut"]
    deck.part(x["part"])
    deck.table_slide(x["dm_sources"]["title"], x["dm_sources"], per_page=7)
    deck.cards(x["dm_challenges"]["title"], x["dm_challenges"]["cards"], cols=3, notes=x["dm_challenges"]["notes"],
               size=14)
    deck.table_slide(x["integration"]["title"], x["integration"], per_page=7)
    deck.cards(x["integration_challenges"]["title"], x["integration_challenges"]["cards"], cols=3,
               notes=x["integration_challenges"]["notes"], size=14)
    deck.table_slide(x["shared"]["title"], x["shared"], per_page=8)

    r = d["roadmap"]
    deck.part(r["part"])
    deck.principles(r["principles"]["title"], r["principles"]["items"], r["principles"]["notes"])
    deck.table_slide(r["automation"]["title"], r["automation"], fill_col=3, fill_map=LEVEL, per_page=7)
    deck.horizons(r["horizons"]["title"], r["horizons"])
    deck.table_slide(r["kpis"]["title"], r["kpis"], per_page=8)
    deck.table_slide(r["open_points"]["title"], r["open_points"], per_page=9)
    deck.content(r["ask"]["title"], r["ask"]["items"], size=18)
    deck.notes(r["ask"]["notes"])

    out = deck.save(OUT)
    print(f"{out} ({deck._n} slides)")
    if previews:
        import render as rnd

        pdf = rnd.to_pdf(out, outdir=Path(previews))
        sheets = rnd.previews(pdf, dpi=60)
        print(f"previews: {sheets[0].parent if sheets else '-'}")
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--previews", nargs="?", const=str(Path(tempfile.gettempdir()) / "bibs_floorwalk_deck"),
                    help="render slide PNGs and contact sheets into this folder (default: a temp folder)")
    args = ap.parse_args()
    build(previews=args.previews)
    return 0


if __name__ == "__main__":
    sys.exit(main())
