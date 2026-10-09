"""Collects the results of a live run of the walkthroughs of the sign-off packs (the recipes of capture_pack.cjs,
which perform each step on the seed profile) into data/wt_results.json, read by build_conformance.py.

For each step of pack/walkthroughs.yaml: performed or failed (the recipe's message), the screen the step ended on,
the line of the screen closest to what the step says the user sees, and whether the wording of the expected
outcome is on the screen (yes, partly, no). The screenshot of a failed step is copied to shots/.

The run folder holds <brd>.log (capture output), text/<brd>/<shot>.json (the text of the screen after each step,
WT_TEXT_DIR of capture_pack) and fail/<brd>/capture-pack-failed-<shot>-<user>.png.

Usage: python3 collect_walkthroughs.py <run folder>
"""

from __future__ import annotations

import json
import re
import shutil
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[5]
HERE = Path(__file__).resolve().parent
SRC = ROOT / "docs/deliverables/src"
STOP = {"the", "a", "an", "of", "and", "or", "to", "on", "for", "in", "with", "is", "its", "it", "by", "at", "as",
        "are", "be", "from", "that", "this", "their", "his", "her", "has", "shows", "show", "message", "tab"}
ERROR = re.compile(r"Something went wrong|could not be (?:loaded|saved)|Internal error|unexpected error", re.I)


def words(text: str) -> set[str]:
    """Significant words of a text; placeholders of the walkthrough (<insurer>, QT-yyyy-nnnnnn) left out."""
    text = re.sub(r"<[^>]*>|\b[A-Z]{2,4}(?:-[A-Z]{2,4})?-yyyy-n+\b|\b(?:Toast|Message):", " ", text)
    return {w for w in re.findall(r"[a-z0-9]+", text.lower()) if w not in STOP and len(w) > 2}


def best_line(text: str, expected: str) -> tuple[str, float]:
    want = words(expected)
    best, score = "", 0.0
    for line in (x.strip() for x in text.splitlines()):
        if not line or len(line) > 220:
            continue
        have = words(line)
        s = len(want & have) / max(len(want), 1)
        if s > score:
            best, score = line, s
    return best, score


def main(run: Path) -> None:
    results = []
    shots = HERE / "shots"
    shots.mkdir(exist_ok=True)
    for pack in sorted(SRC.glob("BRD-*/pack/walkthroughs.yaml")):
        brd = "BRD-" + pack.parts[-3][4:6]
        b = brd.replace("BRD-", "brd")
        log = run / f"{b}.log"
        if not log.exists():
            continue
        outcome: dict[str, str | None] = {}
        for line in log.read_text(encoding="utf-8", errors="replace").splitlines():
            m = re.match(r"(captured|FAILED) (wt-[a-z]-\d+) ?(.*)", line)
            if m:
                outcome[m.group(2)] = None if m.group(1) == "captured" else m.group(3)
        for wt in yaml.safe_load(pack.read_text(encoding="utf-8"))["walkthroughs"]:
            for n, step in enumerate(wt["steps"], start=1):
                persona, screen_id, does, sees, result, slug = (step + [None] * 6)[:6]
                if not slug or slug not in outcome:
                    results.append({"brd": brd, "walkthrough": wt["id"], "step": n, "shot": slug, "status": "not run",
                                    "persona": persona, "screen_id": screen_id, "does": does, "sees": sees, "result": result})
                    continue
                error = outcome[slug]
                text, url = "", ""
                tf = run / "text" / b / f"{slug}.json"
                if tf.exists():
                    d = json.loads(tf.read_text(encoding="utf-8"))
                    text, url = d["text"], d["url"]
                head = " – ".join([x.strip() for x in text.splitlines() if x.strip()][:2])
                line, score = best_line(text, f"{sees} {result}")
                found = str(result or "").lower() in text.lower() or score >= 0.5
                wording = "yes" if found else ("partly" if score >= 0.3 else "no")
                if not text:
                    wording = "not recorded"
                status = "fail" if error or ERROR.search(text) else "pass"
                shot = None
                if status == "fail":
                    for f in sorted((run / "fail" / b).glob(f"capture-pack-failed-{slug}-*.png")):
                        shot = f"{brd}_{wt['id']}_{n}.png"
                        shutil.copyfile(f, shots / shot)
                        break
                results.append({"brd": brd, "walkthrough": wt["id"], "step": n, "shot": slug, "status": status,
                                "persona": persona, "screen_id": screen_id, "does": does, "sees": sees, "result": result, "url": url,
                                "screen": head, "observed": line, "wording": wording, "error": error,
                                "screenshot": shot})
    out = HERE / "data" / "wt_results.json"
    out.write_text(json.dumps(results, indent=1), encoding="utf-8")
    counts: dict[str, int] = {}
    for r in results:
        counts[r["status"]] = counts.get(r["status"], 0) + 1
    print(counts)


if __name__ == "__main__":
    main(Path(sys.argv[1]))
