"""Graphviz sources for the business process deck (swimlanes and enterprise maps).

The diagrams are generated from the YAML data files in ``process_deck/`` and written to
``figures/<name>.dot`` with the BDO colour tokens of ``tools/deliverables/brand.py``; the
toolkit's ``render_dot`` replaces the tokens and renders ``figures/<name>.png``.

Every node is pinned (``layout=neato``, ``pos="x,y!"``, ``inputscale=72``), so the lanes are
straight bands and the drawing is sized in points to the diagram area of a 16:9 slide
(873 x 380 pt): the text is rendered at its real slide size. Edit the YAML, not the .dot files.
"""

from __future__ import annotations

import textwrap
from pathlib import Path
from typing import Any

CANVAS_W = 873.0  # points: 12.13 in, the content width of a 13.33 in slide
CANVAS_H = 372.0  # points: 5.17 in, below the title band, above the caption
FONT_PT = 12
CHAR_PT = 5.8  # average width of an Arial character at 12 pt, with a margin

# Lane header colours by lane kind: (fill, font).
LANE_HEAD = {
    "role": ("@HEADER_BLUE", "@WHITE"),
    "external": ("@MUTED", "@WHITE"),
    "system": ("@FIELD_BLUE", "@NEAR_BLACK"),
    "bibs": ("@CTA_BLUE", "@WHITE"),
}
LANE_BAND = {"role": "@WHITE", "external": "@DIRTY_WHITE", "system": "#EEF3F8", "bibs": "@BG_BLUE"}

# Step styles by kind: (shape, style, fill, border, font colour, bold).
STEP = {
    "manual": ("box", "rounded,filled", "@WHITE", "@HEADER_BLUE", "@NEAR_BLACK", False),
    "legacy": ("box", "rounded,filled,dashed", "@WHITE", "@MUTED", "@NEAR_BLACK", False),
    "external": ("box", "rounded,filled", "#E4E4E4", "@MUTED", "@NEAR_BLACK", False),
    "screen": ("box", "rounded,filled", "@BG_BLUE", "@CTA_BLUE", "@NEAR_BLACK", False),
    "auto": ("box", "rounded,filled", "@CTA_BLUE", "@CTA_BLUE", "@WHITE", True),
    "doc": ("note", "filled", "@WHITE", "@CTA_BLUE", "@NEAR_BLACK", False),
    "area": ("box", "rounded,filled", "@WHITE", "@HEADER_BLUE", "@NEAR_BLACK", False),
    "group": ("box", "rounded,filled", "@HEADER_BLUE", "@HEADER_BLUE", "@WHITE", True),
    "service": ("box", "rounded,filled", "@CTA_BLUE", "@CTA_BLUE", "@WHITE", False),
    "parked": ("box", "rounded,filled,dashed", "@WHITE", "@MUTED", "@MUTED", False),
    "stage": ("box", "rounded,filled", "@BG_BLUE", "@HEADER_BLUE", "@NEAR_BLACK", False),
    "owner": ("box", "rounded,filled", "@WHITE", "@BORDER", "@TEXT", False),
    "module": ("box", "rounded,filled", "@WHITE", "@CTA_BLUE", "@HEADER_BLUE", False),
    "vc_built": ("box", "rounded,filled", "@SUCCESS_BG", "@SUCCESS", "@NEAR_BLACK", True),
    "vc_build": ("box", "rounded,filled", "@AMBER_BG", "#8A5A00", "@NEAR_BLACK", True),
    "vc_designed": ("box", "rounded,filled,dashed", "@BG_BLUE", "@HEADER_BLUE", "@NEAR_BLACK", True),
}

LEGENDS = {
    "asis": [("manual", "Manual step (BDOI)"), ("legacy", "Step in today's system or file"),
             ("external", "External party"), ("pain", "Pain point (see next slide)")],
    "tobe": [("screen", "BDOI user on a BIBS screen"), ("auto", "BIBS does it automatically"),
             ("doc", "Document or notice"), ("external", "External party"), ("parked", "Parked interface")],
}


def _esc(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


def wrap(text: str, width_pt: float, size: float = FONT_PT) -> list[str]:
    chars = max(8, int(width_pt / (CHAR_PT * size / FONT_PT)))
    lines: list[str] = []
    for part in str(text).split("\n"):
        lines.extend(textwrap.wrap(part, chars, break_on_hyphens=False, break_long_words=False) or [""])
    return lines


def _label(lines: list[str], colour: str, bold: bool, size: float, sub: list[str] | None = None) -> str:
    body = "<br/>".join(_esc(x) for x in lines)
    if bold:
        body = f"<b>{body}</b>"
    html = f'<font point-size="{size}" color="{colour}">{body}</font>'
    if sub:
        html += f'<br/><font point-size="{size - 1}" color="@MUTED">{"<br/>".join(_esc(x) for x in sub)}</font>'
    return f"<{html}>"


class Canvas:
    """Pinned nodes and straight edges; y grows downwards in the API and is flipped on output."""

    def __init__(self, name: str, comment: str, width: float = CANVAS_W, height: float = CANVAS_H):
        self.name, self.comment, self.w, self.h = name, comment, width, height
        self.nodes: list[str] = []
        self.edges: list[str] = []
        self.boxes: dict[str, tuple[float, float, float, float]] = {}
        self._n = 0

    def _id(self, prefix: str) -> str:
        self._n += 1
        return f"{prefix}{self._n}"

    def rect(self, x: float, y: float, w: float, h: float, fill: str, border: str = "@BORDER",
             label: str = "", font: str = "@NEAR_BLACK", size: float = FONT_PT, bold: bool = False,
             style: str = "filled", shape: str = "box", node_id: str | None = None, penwidth: float = 0.8,
             sub: list[str] | None = None, wrap_text: bool = True) -> str:
        nid = node_id or self._id("r")
        raw_sub = sub
        while True:
            lines = wrap(label, w - 12, size * (1.1 if bold else 1.0)) if (label and wrap_text) else ([label] if label else [])
            sub = [x for line in raw_sub for x in wrap(line, w - 10, size - 1)] if raw_sub else None
            height = len(lines) * size * 1.2 + (len(sub) * (size - 1) * 1.2 if sub else 0)
            # shrink a crowded label by half points, never below 10.5 pt
            if height <= h - 6 or size <= 10.5 or not wrap_text:
                break
            size -= 0.5
        lab = _label(lines, font, bold, size, sub) if label else '""'
        cx, cy = x + w / 2, self.h - (y + h / 2)
        self.nodes.append(
            f'  {nid} [pos="{cx:.1f},{cy:.1f}!", shape={shape}, style="{style}", fillcolor="{fill}", '
            f'color="{border}", penwidth={penwidth}, width={w / 72:.3f}, height={h / 72:.3f}, '
            f'fixedsize=true, margin="0.03,0.02", label={lab}];')
        self.boxes[nid] = (x, y, w, h)
        return nid

    def step(self, node_id: str, x: float, y: float, w: float, h: float, text: str, kind: str,
             size: float = FONT_PT, sub: list[str] | None = None) -> str:
        shape, style, fill, border, font, bold = STEP[kind]
        return self.rect(x, y, w, h, fill, border, text, font, size, bold, style, shape, node_id,
                         penwidth=1.3, sub=sub)

    def marker(self, x: float, y: float, text: str, d: float = 19) -> None:
        nid = self._id("m")
        self.nodes.append(
            f'  {nid} [pos="{x:.1f},{self.h - y:.1f}!", shape=circle, style=filled, fillcolor="@YELLOW", '
            f'color="@NEAR_BLACK", penwidth=0.8, width={d / 72:.3f}, height={d / 72:.3f}, fixedsize=true, '
            f'label=<<font point-size="11" color="@NEAR_BLACK"><b>{_esc(text)}</b></font>>];')

    def edge(self, a: str, b: str, style: str = "solid", label: str = "", colour: str = "@HEADER_BLUE",
             ports: tuple[str, str] | None = None, width: float = 1.3, arrow: bool = True) -> None:
        if ports is None:
            ports = self._ports(a, b)
        attrs = [f'color="{colour}"', f"style={style}", f"penwidth={width}", "arrowsize=0.7"]
        if not arrow:
            attrs.append("arrowhead=none")
        if ports[0]:
            attrs.append(f"tailport={ports[0]}")
        if ports[1]:
            attrs.append(f"headport={ports[1]}")
        if label:
            attrs.append(f'label=<<font point-size="10" color="@MUTED">{_esc(label)}</font>>')
        self.edges.append(f"  {a} -> {b} [{', '.join(attrs)}];")

    def _ports(self, a: str, b: str) -> tuple[str, str]:
        ax, ay, aw, ah = self.boxes[a]
        bx, by, bw, bh = self.boxes[b]
        if bx >= ax + aw - 1:
            return "e", "w"
        if bx + bw <= ax + 1:
            return "w", "e"
        return ("s", "n") if by > ay else ("n", "s")

    def legend(self, items: list[tuple[str, str]], y: float, x0: float = 0.0) -> None:
        x = x0
        for kind, text in items:
            if kind == "pain":
                self.marker(x + 9, y + 9, "1", d=17)
                tx = x + 22
            else:
                shape, style, fill, border, font, bold = STEP[kind]
                self.rect(x, y + 1, 26, 16, fill, border, "", style=style, shape=shape, penwidth=1.2)
                tx = x + 31
            tw = len(text) * 5.1 + 4
            self.rect(tx, y, tw, 18, "@WHITE", "@WHITE", text, "@TEXT", 10.5, style="filled",
                      wrap_text=False)
            x = tx + tw + 14

    def dot(self) -> str:
        head = [f"// {line}" for line in self.comment.splitlines()]
        head += ["// Generated by docs/deliverables/src/decks/build_process_deck.py from the YAML in",
                 "// process_deck/; edit the YAML and rebuild. Colour tokens are replaced at render time."]
        return "\n".join(head + [
            f"digraph {self.name} {{",
            '  graph [layout=neato, inputscale=72, splines=line, overlap=true, outputorder=nodesfirst, '
            'bgcolor="white", pad=0.05, fontname="Arial"];',
            '  node [fontname="Arial"];',
            '  edge [fontname="Arial"];',
            # two invisible corner nodes fix the canvas size
            f'  c0 [pos="0,0!", shape=point, width=0.01, style=invis];',
            f'  c1 [pos="{self.w:.1f},{self.h:.1f}!", shape=point, width=0.01, style=invis];',
            *self.nodes, *self.edges, "}", ""])


# ---------------------------------------------------------------- swimlane

def swimlane(name: str, spec: dict[str, Any], mode: str, comment: str) -> str:
    """Swimlane from {lanes: [{id, label, kind}], cols, steps: [{id, lane, col, text, kind, pain}],
    edges: [[a, b, style?, label?]]}. ``mode`` is asis or tobe (legend)."""
    lanes = spec["lanes"]
    ncols = spec.get("cols") or max(s["col"] for s in spec["steps"])
    legend_h = 24
    head_w = 104.0
    top = 0.0
    lane_h = (CANVAS_H - legend_h - 6) / len(lanes)
    col_w = (CANVAS_W - head_w - 4) / ncols
    cv = Canvas(name, comment)
    lane_y: dict[str, float] = {}
    for i, lane in enumerate(lanes):
        y = top + i * lane_h
        lane_y[lane["id"]] = y
        kind = lane.get("kind", "role")
        fill, font = LANE_HEAD[kind]
        cv.rect(head_w, y, CANVAS_W - head_w, lane_h, LANE_BAND[kind], "@BORDER", penwidth=0.6)
        cv.rect(0, y, head_w - 2, lane_h, fill, "@WHITE", lane["label"], font, 11.5, True, penwidth=0.6)
    box_h = min(62.0, lane_h - 10)
    for s in spec["steps"]:
        span = s.get("span", 1)
        w = col_w * span - 10
        x = head_w + (s["col"] - 1) * col_w + 5 + s.get("dx", 0)
        y = lane_y[s["lane"]] + (lane_h - box_h) / 2
        kind = s.get("kind") or ("manual" if mode == "asis" else "screen")
        cv.step(s["id"], x, y, w, box_h, s["text"], kind)
        for k, p in enumerate(reversed(s.get("pain", []) or [])):
            cv.marker(x + w - 4 - k * 21, y + 2, str(p))
    where = {s["id"]: ([ln["id"] for ln in lanes].index(s["lane"]), s["col"]) for s in spec["steps"]}
    for e in spec.get("edges", []):
        a, b = e[0], e[1]
        style = e[2] if len(e) > 2 and e[2] else "solid"
        label = e[3] if len(e) > 3 else ""
        (la, ca), (lb, cb) = where[a], where[b]
        if abs(la - lb) + abs(ca - cb) <= 1:  # neighbouring boxes: no room for a label
            label = ""
        colour = "@MUTED" if style == "dashed" else "@HEADER_BLUE"
        cv.edge(a, b, style=style, label=label, colour=colour)
    cv.legend(LEGENDS[mode], CANVAS_H - legend_h + 4, x0=head_w)
    return cv.dot()


# ---------------------------------------------------------------- free layout

def free_layout(name: str, spec: dict[str, Any], comment: str) -> str:
    """Enterprise maps: {bands: [{x, y, w, h, label, fill}], nodes: [{id, x, y, w, h, text, kind, sub,
    size, pain}], edges: [{a, b, style, label, colour, ports, width, arrow}], legend: [[kind, text]]},
    in points from the top left."""
    cv = Canvas(name, comment, spec.get("width", CANVAS_W), spec.get("height", CANVAS_H))
    for b in spec.get("bands", []):
        cv.rect(b["x"], b["y"], b["w"], b["h"], b.get("fill", "@DIRTY_WHITE"), b.get("border", "@BORDER"),
                penwidth=0.8)
        if b.get("label"):
            ly = b["y"] + b["h"] - 21 if b.get("label_pos") == "bottom" else b["y"] + 3
            cv.rect(b["x"] + 4, ly, b.get("label_w", b["w"] - 8), 18, b.get("fill", "@DIRTY_WHITE"),
                    b.get("fill", "@DIRTY_WHITE"), b["label"], b.get("label_colour", "@HEADER_BLUE"), 11.5,
                    True, wrap_text=False)
    for n in spec.get("nodes", []):
        size = n.get("size", FONT_PT)
        cv.step(n["id"], n["x"], n["y"], n["w"], n["h"], n["text"], n.get("kind", "area"), size=size,
                sub=n.get("sub"))
        for k, p in enumerate(n.get("pain", []) or []):
            cv.marker(n["x"] + n["w"] - 4 - k * 21, n["y"] + 2, str(p))
    for e in spec.get("edges", []):
        style = e.get("style", "solid")
        colour = e.get("colour") or ("@MUTED" if style == "dashed" else "@HEADER_BLUE")
        cv.edge(e["a"], e["b"], style=style, label=e.get("label", ""), colour=colour,
                ports=e.get("ports"), width=e.get("width", 1.3), arrow=e.get("arrow", True))
    if spec.get("legend"):
        cv.legend([tuple(x) for x in spec["legend"]], cv.h - 22, x0=spec.get("legend_x", 0))
    return cv.dot()


def write(path: Path, text: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    return path
