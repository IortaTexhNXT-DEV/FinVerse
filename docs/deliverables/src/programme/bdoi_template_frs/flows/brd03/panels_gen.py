"""Page generator of the one-page "Product Maintenance - Processes at a Glance" picture (panel layout; the generator
of flows/brd11, the same theme).

    python3 panels_gen.py panels.py OUT.html     # render_panels.sh renders it to PNG

The SPEC module gives the header, the panels (grid cell, number, title, subtitle, body) and the legend. A body is
a list of blocks: ("flow", [blocks]) a small vertical flow, ("cols", [blocks]) side-by-side blocks, ("box", title,
[lines], kind) a box, ("list", [lines]) a bullet list, ("arrow", text) a right arrow. Tags inside text:
{CR:CR-PM-nnn} orange, {OOTB} green, {INT:text} grey label; untagged text is BRD (black); "|" is a line break.
"""
import base64, html, importlib.util, re, sys
from pathlib import Path

LOGO = Path(__file__).resolve().parents[7] / "frontend" / "src" / "assets" / "platform" / "iorta-technxt.png"
NAVY = "#0D2F5E"
KIND = {"": ("#FFFFFF", "#90A4AE"), "id": ("#E1F5FE", "#0277BD"), "sys": ("#E8F5E9", "#2E7D32"),
        "rej": ("#FFEBEE", "#C62828"), "ret": ("#EDE7F6", "#5E35B1"), "apr": ("#FFF8E1", "#F9A825"),
        "aud": ("#F3E5F5", "#7B1FA2"), "dec": ("#FFFDE7", "#F9A825")}


def tags(s: str) -> str:
    s = html.escape(s, quote=False)
    s = re.sub(r"\{CR:([^}]+)\}", r'<span class="cr">\1</span>', s)
    s = s.replace("{OOTB}", '<span class="ootb">OOTB</span>')
    s = re.sub(r"\{INT:([^}]+)\}", r'<span class="int">[\1]</span>', s)
    return s.replace("|", "<br>")


def block(b) -> str:
    k = b[0]
    if k == "flow":
        return '<div class="flow">' + '<div class="dn">&#9660;</div>'.join(block(s) for s in b[1]) + "</div>"
    if k == "cols":
        return '<div class="cols">' + "".join(f'<div class="c">{block(s)}</div>' for s in b[1]) + "</div>"
    if k == "list":
        return "<ul>" + "".join(f"<li>{tags(x)}</li>" for x in b[1]) + "</ul>"
    if k == "arrow":
        return f'<div class="ra">{tags(b[1])}<span>&#10132;</span></div>'
    if k == "box":
        _, title, lines, kind = b
        fill, bd = KIND[kind]
        body = "<ul>" + "".join(f"<li>{tags(x)}</li>" for x in lines) + "</ul>" if lines else ""
        t = f'<div class="t">{tags(title)}</div>' if title else ""
        return f'<div class="box{" d" if kind == "dec" else ""}" style="background:{fill};border-color:{bd}">{t}{body}</div>'
    raise ValueError(k)


def page(spec: dict) -> str:
    W, H = spec["size"]
    logo = "data:image/png;base64," + base64.b64encode(LOGO.read_bytes()).decode("ascii")
    panels = []
    for p in spec["panels"]:
        c1, r1, cs, rs = p["grid"]
        panels.append(f'<section style="grid-column:{c1}/span {cs};grid-row:{r1}/span {rs};--c:{p["colour"]};--f:{p["fill"]}">'
                      f'<h2><b>{p["no"]}</b><span>{tags(p["title"])}<i>{tags(p.get("sub", ""))}</i></span></h2>'
                      f'<div class="pb">{"".join(block(b) for b in p["body"])}</div></section>')
    legend = "".join(f'<span class="lg">{tags(x)}</span>' for x in spec["legend"])
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>
*{{box-sizing:border-box}} body{{margin:0;width:{W}px;height:{H}px;font-family:'DejaVu Sans',Arial,sans-serif;background:#fff;color:#111827;overflow:hidden;position:relative}}
.hdr{{position:absolute;left:10px;right:10px;top:10px;height:70px;background:{NAVY};border-radius:8px;display:flex;align-items:center;padding:0 16px;color:#fff}}
.hdr img{{height:48px;background:#fff;border-radius:6px;padding:4px 8px;margin-right:18px}}
.hdr h1{{font-size:27px;margin:0;flex:1}} .hdr .tag{{font-size:14px;border:1.5px solid #fff;border-radius:6px;padding:6px 10px;line-height:1.35;text-align:right}}
main{{position:absolute;left:10px;right:10px;top:88px;bottom:38px;display:grid;grid-template-columns:repeat(12,minmax(0,1fr));grid-template-rows:{spec['rows']};gap:8px}}
section{{border:2px solid var(--c);border-radius:9px;background:var(--f);display:flex;flex-direction:column;min-height:0;overflow:hidden}}
h2{{margin:0;display:flex;align-items:center;gap:8px;padding:5px 8px 3px;font-size:17px;color:var(--c)}}
h2 b{{flex:none;width:30px;height:28px;border-radius:14px;background:var(--c);color:#fff;display:flex;align-items:center;justify-content:center;font-size:14px}}
h2 i{{display:block;font-style:normal;font-weight:400;font-size:12.5px;color:#374151}}
.pb{{flex:1;padding:3px 8px 7px;display:flex;flex-direction:column;gap:5px;min-height:0}}
.cols{{display:flex;gap:7px;align-items:stretch}} .cols>.c{{flex:1;display:flex;flex-direction:column;gap:5px;min-width:0}}
.cols>.c:has(>.ra){{flex:none}}
.flow{{display:flex;flex-direction:column;align-items:stretch}} .dn{{text-align:center;color:#455A64;font-size:10px;line-height:11px}}
.box{{border:1.6px solid;border-radius:6px;padding:3px 6px;font-size:13.5px;line-height:1.25}}
.box.d{{border-style:dashed}} .box .t{{font-weight:700;font-size:14px}}
ul{{margin:0;padding-left:15px;font-size:13.5px;line-height:1.27}} .box ul{{padding-left:13px}}
.ra{{display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;font-size:12px;text-align:center;color:{NAVY};font-weight:700;width:84px}}
.ra span{{font-size:26px;line-height:26px}}
.cr{{display:inline-block;background:#EF6C00;color:#fff;font-size:11px;font-weight:700;border-radius:3px;padding:0 4px;margin-left:3px}}
.ootb{{display:inline-block;background:#2E7D32;color:#fff;font-size:11px;font-weight:700;border-radius:3px;padding:0 4px;margin-left:3px}}
.int{{color:#6B7280;font-size:11.5px;font-weight:700}}
footer{{position:absolute;left:12px;right:12px;bottom:8px;display:flex;gap:18px;align-items:center;font-size:12.5px}}
.lg{{white-space:nowrap}}
</style></head><body>
<div class="hdr"><img src="{logo}"><h1>{tags(spec['title'])}</h1><div class="tag">{tags(spec['tag'])}</div></div>
<main>{"".join(panels)}</main><footer><b>Legend:</b>{legend}</footer></body></html>"""


if __name__ == "__main__":
    sp = importlib.util.spec_from_file_location("spec", sys.argv[1])
    m = importlib.util.module_from_spec(sp)
    sp.loader.exec_module(m)
    Path(sys.argv[2]).write_text(page(m.SPEC), encoding="utf-8")
