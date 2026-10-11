"""Figures of the User Access Maintenance FRS in BDOI's template, version 1.3 (Graphviz, PNG at 240 dpi).

    python docs/deliverables/src/programme/bdoi_template_frs/brd11_figures.py OUT_DIR   # renders every figure

Three kinds of figure, all described in brd11_flows.yaml:

  swimlane   a process with one vertical lane per persona (and the system), the steps placed in rows, decision
             diamonds, start and end points and SLA notes. The layout is computed here (lanes, rows, orthogonal
             connectors) and drawn by Graphviz with fixed positions (neato -n2), so that the lanes keep their order
             and every step stays in its lane, readable at A4 width;
  states     a status life-cycle of a record (dot, left to right);
  context    the integration context (dot), the system in the middle and the other systems and modules around.

Every figure is rendered into OUT_DIR as <id>.png; the caption and the "how to read" line stay in the YAML and are
written under the figure by the FRS builder.
"""

from __future__ import annotations

import subprocess
import sys
import textwrap
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
SPEC = HERE / "brd11_flows.yaml"

DPI = 240
FONT = "Arial"
INK = "#1F3864"
LANE_FILLS = ["#EAF1FB", "#FFF7E0", "#EEF5EA", "#F6ECF7", "#FDEDEC", "#EAF6F6", "#F2F2F2"]
LANE_HEAD = "#1F3864"
SYSTEM_FILL = "#E2EFDA"

# Geometry of a swim-lane figure, in points (72 per inch).
LANE_W = 122.0          # width of a lane
HEAD_H = 30.0           # height of the lane header
ROW_H = 58.0            # height of a row
NODE_W = 104.0
NODE_H = 38.0
DIAMOND_W = 92.0
DIAMOND_H = 44.0
MARGIN = 8.0


def _esc(s: str) -> str:
    return str(s).replace("\\", "\\\\").replace('"', '\\"')


def _html(s: str) -> str:
    return str(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def _cell(s: str, width: int) -> str:
    return "<BR ALIGN=\"LEFT\"/>".join(_html(x) for x in textwrap.wrap(str(s), width) or [""]) + \
        ("<BR ALIGN=\"LEFT\"/>" if len(str(s)) > width else "")


def _wrap(text: str, width: int = 20) -> str:
    out = []
    for part in str(text).split("\n"):
        out += textwrap.wrap(part, width=width) or [""]
    return "\\n".join(_esc(x) for x in out)


def _spline(points: list[tuple[float, float]]) -> str:
    """A Graphviz edge position (B-spline) drawing straight segments through the points."""
    pts = [points[0]]
    for (x1, y1), (x2, y2) in zip(points, points[1:]):
        pts += [(x1 + (x2 - x1) / 3, y1 + (y2 - y1) / 3), (x1 + 2 * (x2 - x1) / 3, y1 + 2 * (y2 - y1) / 3), (x2, y2)]
    end = pts[-1]
    body = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts[:-1])
    # the arrow head: the spline stops a little before the end point
    (px, py) = pts[-2]
    dx, dy = end[0] - px, end[1] - py
    n = max((dx * dx + dy * dy) ** 0.5, 0.01)
    stop = (end[0] - dx / n * 7, end[1] - dy / n * 7)
    pts[-1] = stop
    body = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
    return f"e,{end[0]:.1f},{end[1]:.1f} {body}"


class Swimlane:
    def __init__(self, spec: dict):
        self.spec = spec
        self.lanes = spec["lanes"]
        self.lane_ix = {lane["id"]: i for i, lane in enumerate(self.lanes)}
        self.steps = {s["id"]: s for s in spec["steps"]}
        self.lw = min(LANE_W, 490.0 / len(self.lanes))      # the figure fits the A4 text width at 100 %
        self.nw = min(NODE_W, self.lw - 14)
        self.chars = max(10, int(self.nw / 4.6))
        rows = max(int(s["row"]) for s in spec["steps"]) + 1
        self.row_h = [ROW_H] * rows
        for sid, s in self.steps.items():
            r = int(s["row"])
            self.row_h[r] = max(self.row_h[r], self.size(sid)[1] + 20)
        self.height = HEAD_H + sum(self.row_h) + MARGIN
        self.width = len(self.lanes) * self.lw

    def centre(self, sid: str) -> tuple[float, float]:
        s = self.steps[sid]
        r = int(s["row"])
        x = self.lane_ix[s["lane"]] * self.lw + self.lw / 2 + float(s.get("dx", 0)) * self.lw
        y = self.height - HEAD_H - sum(self.row_h[:r]) - self.row_h[r] / 2     # Graphviz: y grows upwards
        return x, y

    def size(self, sid: str) -> tuple[float, float]:
        kind = self.steps[sid].get("kind", "task")
        if kind == "decision":
            return min(DIAMOND_W, self.lw - 10), DIAMOND_H
        if kind in ("start", "end"):
            return min(64.0, self.nw), 26.0
        lines = len(textwrap.wrap(str(self.steps[sid].get("text", "")), self.chars)) or 1
        return self.nw, max(NODE_H, 10.5 * lines + 8)

    def ports(self, sid: str) -> dict[str, tuple[float, float]]:
        x, y = self.centre(sid)
        w, h = self.size(sid)
        return {"top": (x, y + h / 2), "bottom": (x, y - h / 2), "left": (x - w / 2, y), "right": (x + w / 2, y)}

    def route(self, a: str, b: str, k: int, port: str | None = None):
        """Orthogonal connector from a to b, and the position of its label."""
        sa, sb = self.steps[a], self.steps[b]
        ra, rb = int(sa["row"]), int(sb["row"])
        la, lb = self.lane_ix[sa["lane"]], self.lane_ix[sb["lane"]]
        pa, pb = self.ports(a), self.ports(b)
        (xa, ya), (xb, yb) = self.centre(a), self.centre(b)
        if port in ("left", "right") and abs(xa - xb) > 1:
            x0, y0 = pa[port]
            if rb > ra:
                pts = [(x0, y0), (xb, y0), pb["top"]]
            elif rb < ra:
                pts = [(x0, y0), (xb, y0), pb["bottom"]]
            else:
                pts = [(x0, y0), pb["left" if xb > xa else "right"]]
        elif port in ("left", "right"):         # same column: along the gutter of the lane
            x0, y0 = pa[port]
            edge = la * self.lw + 3 if port == "left" else (la + 1) * self.lw - 3
            pts = [(x0, y0), (edge, y0), (edge, yb), pb[port]]
        elif rb > ra and abs(xa - xb) < 1:
            pts = [pa["bottom"], pb["top"]]
        elif rb > ra:
            x0, y0 = pa["bottom"]
            x1, y1 = pb["top"]
            gap = (self.row_h[ra] - self.size(a)[1]) / 2
            ymid = y0 - gap + 4 + (k % 2) * 3
            pts = [(x0, y0), (x0, ymid), (x1, ymid), (x1, y1)]
        elif rb == ra:
            pts = [pa["right"], pb["left"]] if xb > xa else [pa["left"], pb["right"]]
        elif la == lb:                           # back to an earlier row of the same lane: left gutter
            x0, y0 = pa["left"]
            edge = la * self.lw + 3
            pts = [(x0, y0), (edge, y0), (edge, yb), pb["left"]]
        else:                                    # back to an earlier row of another lane
            x0, y0 = pa["top"]
            gap = (self.row_h[ra] - self.size(a)[1]) / 2
            ymid = y0 + gap - 4
            x1, y1 = pb["bottom"]
            pts = [(x0, y0), (x0, ymid), (x1, ymid), (x1, y1)]
        p0, p1 = pts[0], pts[1]
        if len(pts) > 2 and abs(p0[0] - p1[0]) + abs(p0[1] - p1[1]) < 25:
            p0, p1 = pts[1], pts[2]
        mid = ((p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2)
        if abs(p0[0] - p1[0]) < 1:               # vertical first segment: label beside it
            mid = (p0[0] + 4, p0[1] - min(12, abs(p0[1] - p1[1]) / 2))
        return pts, mid

    def dot(self) -> str:
        L = [f'digraph "{self.spec["id"]}" {{',
             f'graph [splines=true, bgcolor=white, pad=0.1, outputorder=nodesfirst, '
             f'bb="0,0,{self.width:.0f},{self.height:.0f}"];',
             f'node [fontname="{FONT}", fontsize=8, fixedsize=true, color="{INK}", penwidth=1.0];',
             f'edge [color="#404040", arrowsize=0.6, penwidth=0.9, fontname="{FONT}", fontsize=7.5, '
             f'fontcolor="#7F3F00"];']
        for i, lane in enumerate(self.lanes):
            fill = SYSTEM_FILL if lane.get("system") else LANE_FILLS[i % len(LANE_FILLS)]
            body_h = self.height - HEAD_H
            x = i * self.lw + self.lw / 2
            L.append(f'lane{i} [label="", shape=box, style=filled, fillcolor="{fill}", color="#8EA9DB", '
                     f'width={self.lw / 72:.3f}, height={body_h / 72:.3f}, pos="{x:.1f},{body_h / 2:.1f}!"];')
            L.append(f'head{i} [label="{_wrap(lane["label"], max(10, int(self.lw / 5.2)))}", shape=box, '
                     f'style=filled, fillcolor="{LANE_HEAD}", fontcolor=white, fontname="{FONT} Bold", fontsize=8, '
                     f'color="{LANE_HEAD}", width={self.lw / 72:.3f}, height={HEAD_H / 72:.3f}, '
                     f'pos="{x:.1f},{self.height - HEAD_H / 2:.1f}!"];')
        for sid, s in self.steps.items():
            x, y = self.centre(sid)
            w, h = self.size(sid)
            kind = s.get("kind", "task")
            label = _wrap(s.get("text", ""), max(9, self.chars - 5) if kind == "decision" else self.chars)
            style = {
                "decision": 'shape=diamond, style=filled, fillcolor="#FFFFFF"',
                "start": 'shape=ellipse, style=filled, fillcolor="#C6E0B4"',
                "end": 'shape=ellipse, style="filled,bold", fillcolor="#F8CBAD"',
                "note": 'shape=note, style=filled, fillcolor="#FFF2CC", color="#BF9000", fontsize=7.5',
                "system": 'shape=box, style="rounded,filled", fillcolor="#FFFFFF", color="#548235", penwidth=1.3',
            }.get(kind, 'shape=box, style="rounded,filled", fillcolor="#FFFFFF"')
            L.append(f'{sid} [label="{label}", {style}, width={w / 72:.3f}, height={h / 72:.3f}, '
                     f'pos="{x:.1f},{y:.1f}!"];')
        for k, e in enumerate(self.spec.get("edges", [])):
            a, b = e[0], e[1]
            lab = e[2] if len(e) > 2 else ""
            style = e[3] if len(e) > 3 else ""
            port = e[4] if len(e) > 4 else None
            pts, mid = self.route(a, b, k, port)
            attrs = [f'pos="{_spline(pts)}"']
            if lab:
                attrs.append(f'label="{_esc(lab)}", lp="{mid[0] + 2:.1f},{mid[1] + 5:.1f}"')
            if style == "dashed":
                attrs.append("style=dashed")
            L.append(f'{a} -> {b} [{", ".join(attrs)}];')
        L.append("}")
        return "\n".join(L)


def render(spec: dict, out: Path) -> Path:
    png = out / f"{spec['id']}.png"
    if spec["kind"] == "swimlane":
        src = Swimlane(spec).dot()
        cmd = ["neato", "-n2", "-Tpng", f"-Gdpi={DPI}"]
    else:
        src = spec["dot"]
        if "@ROWS@" in src:
            head = "".join(f'<TD BGCOLOR="{LANE_HEAD}" ALIGN="{"LEFT" if i == 0 else "CENTER"}"><FONT COLOR="white">'
                           f'<B>{"<BR/>".join(_html(x) for x in str(h).split("|"))}</B></FONT></TD>'
                           for i, h in enumerate(spec["header"]))
            rows = []
            for i, r in enumerate(spec["rows"]):
                bg = "#EAF1FB" if i % 2 else "#FFFFFF"
                cells = [f'<TD ALIGN="LEFT" BGCOLOR="{bg}">{_cell(r[0], 34)}</TD>']
                if spec.get("tick"):
                    cells += [f'<TD BGCOLOR="{bg}">{"&#10004;" if v else ""}</TD>' for v in r[1:]]
                else:
                    cells += [f'<TD ALIGN="LEFT" BGCOLOR="{bg}">{_cell(v, 22)}</TD>' for v in r[1:]]
                rows.append("<TR>" + "".join(cells) + "</TR>")
            src = src.replace("@HEADER@", "<TR>" + head + "</TR>").replace("@ROWS@", "\n".join(rows))
        cmd = ["dot", "-Tpng", f"-Gdpi={DPI}"]
    res = subprocess.run(cmd + ["-o", str(png)], input=src.encode("utf-8"), capture_output=True)
    if res.returncode != 0:
        raise SystemExit(f"figure {spec['id']}: {res.stderr.decode('utf-8', 'ignore')[:500]}")
    return png


def figures() -> list[dict]:
    return yaml.safe_load(SPEC.read_text(encoding="utf-8"))["figures"]


def render_all(out: Path) -> dict[str, Path]:
    out.mkdir(parents=True, exist_ok=True)
    return {f["id"]: render(f, out) for f in figures()}


if __name__ == "__main__":
    target = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    for fid, p in render_all(target).items():
        print(fid, p)
