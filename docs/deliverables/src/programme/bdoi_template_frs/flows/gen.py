"""Swimlane process-flow pictures of the BDOI-template FRS (HTML page, rendered to PNG by Chromium).

A spec module defines SPEC = dict(size, foot, title, tag, stages, lanes, boxes, edges, footer_html):
  lanes  (name, sub-title, relative height)
  boxes  (id, lane, column, x-offset, y-offset, width (fractions of the column / lane), kind, title, lines[, CR tags])
         kinds: id req apr sys rej ret ntf aud dec; CR tags "CR-x" (yellow: not in any BRD), "EXT:CR-x" (blue:
         extends a BRD requirement), "BRD:ref" (green: already a BRD requirement)
  edges  [from, to, colour kind n|r|t|i|o|a, label, route ''|down|up|vd|vu|hd|hu|lu|topY|botY, optional [x, y] label]
Used by render.py; the picture carries the iorta TechNXT logo (logo.png next to the HTML page).
"""
import html, json, sys

BLUE = "#1565C0"; NAVY = "#0D2F5E"

STAGE_COLS = ["#E3F2FD", "#E8F5E9", "#FFF8E1", "#E0F2F1", "#FFF3E0", "#F3E5F5"]
STAGE_HEAD = ["#90CAF9", "#A5D6A7", "#FFE082", "#80CBC4", "#FFCC80", "#CE93D8"]
KIND = {  # fill, border
    "id": ("#E1F5FE", "#0277BD"), "req": ("#E3F2FD", "#1565C0"), "apr": ("#FFF8E1", "#F9A825"),
    "sys": ("#E8F5E9", "#2E7D32"), "rej": ("#FFEBEE", "#C62828"), "ret": ("#EDE7F6", "#5E35B1"),
    "ntf": ("#FFF3E0", "#EF6C00"), "aud": ("#F3E5F5", "#7B1FA2"), "dec": ("#FFFDE7", "#F9A825"),
}


def page(spec):
    W, H = spec["size"]
    lanes, stages = spec["lanes"], spec["stages"]
    LW, TOP, HEADH = 150, 96, 44
    body_h = H - TOP - HEADH - spec.get("foot", 0) - 10
    colw = (W - LW - 20) / len(stages)
    tot = sum(l[2] for l in lanes)
    lane_y, y = [], TOP + HEADH
    for l in lanes:
        h = body_h * l[2] / tot
        lane_y.append((y, h)); y += h
    out = []
    for i, s in enumerate(stages):
        x = 10 + LW + i * colw
        out.append(f'<div class="stage" style="left:{x}px;top:{TOP}px;width:{colw-4}px;background:{STAGE_HEAD[i]}">{s}</div>')
        out.append(f'<div class="col" style="left:{x}px;top:{TOP+HEADH}px;width:{colw}px;height:{body_h}px;background:{STAGE_COLS[i]}55"></div>')
    for (name, sub, _), (ly, lh) in zip(lanes, lane_y):
        out.append(f'<div class="lane" style="top:{ly}px;height:{lh}px;width:{W-20}px"></div>')
        out.append(f'<div class="lanelab" style="top:{ly}px;height:{lh}px;width:{LW}px"><b>{name}</b><span>{sub}</span></div>')
    for b in spec["boxes"]:
        bid, lane, col, fx, fy, fw, kind, title, lines = b[:9]
        cr = b[9] if len(b) > 9 else None
        ly, lh = lane_y[lane]
        x = 10 + LW + col * colw + fx * colw
        yy = ly + fy * lh
        fill, bd = KIND[kind]
        items = "".join(f"<li>{t}</li>" for t in lines)
        crh = "".join(f'<span class="cr {"brd" if c.startswith("BRD") else ("ext" if c.startswith("EXT") else "")}">{c.split(":",1)[-1]}</span>' for c in (cr or []))
        cls = "box dec" if kind == "dec" else "box"
        body = f'<ul>{items}</ul>' if lines else ""
        out.append(f'<div id="{bid}" class="{cls}" style="left:{x}px;top:{yy}px;width:{fw*colw}px;background:{fill};border-color:{bd}"><div class="t">{title}{crh}</div>{body}</div>')
    foot = spec.get("footer_html", "")
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>
*{{box-sizing:border-box}} body{{margin:0;width:{W}px;height:{H}px;font-family:'DejaVu Sans',Arial,sans-serif;background:#fff;position:relative;overflow:hidden;color:#1f2937}}
.hdr{{position:absolute;left:10px;top:10px;width:{W-20}px;height:74px;background:{NAVY};border-radius:8px;display:flex;align-items:center;padding:0 18px;color:#fff}}
.hdr img{{height:50px;background:#fff;border-radius:6px;padding:4px 8px;margin-right:18px}}
.hdr h1{{font-size:27px;margin:0;flex:1}} .hdr .tag{{font-size:12px;border:1.5px solid #fff;border-radius:6px;padding:5px 9px;line-height:1.35;text-align:right}}
.stage{{position:absolute;height:{HEADH-6}px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:16px;color:{NAVY};border-radius:4px;clip-path:polygon(0 0,94% 0,100% 50%,94% 100%,0 100%,4% 50%)}}
.col{{position:absolute;border-left:1px solid #d6dbe3}}
.lane{{position:absolute;left:10px;border-top:1px solid #c5ccd6}}
.lanelab{{position:absolute;left:10px;background:#F1F4F8;border-right:1px solid #c5ccd6;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;font-size:15px;color:{NAVY};padding:6px}}
.lanelab span{{font-size:11.5px;color:#4b5563;margin-top:3px}}
.box{{position:absolute;border:1.8px solid;border-radius:7px;padding:5px 7px;font-size:11.5px;line-height:1.28;box-shadow:0 1px 2px #0002;z-index:2}}
.box .t{{font-weight:700;font-size:12.5px;margin-bottom:2px}} .box ul{{margin:0;padding-left:14px}}
.box.dec{{border-radius:22px;text-align:center}}
.cr{{display:inline-block;background:#FFD54F;color:#5d4300;font-size:10px;font-weight:700;border-radius:3px;padding:0 4px;margin-left:4px}}
.cr.brd{{background:#C8E6C9;color:#1B5E20}} .cr.ext{{background:#BBDEFB;color:#0D47A1}}
svg{{position:absolute;left:0;top:0;z-index:1}}
.lbl{{font:700 11px 'DejaVu Sans',Arial;}}
.foot span{{white-space:nowrap}} .foot{{position:absolute;left:10px;padding-right:14px;top:{H-spec.get('foot',0)-6}px;width:{W-20}px}}
</style></head><body>
<div class="hdr"><img src="logo.png"><h1>{spec['title']}</h1><div class="tag">{spec['tag']}</div></div>
{''.join(out)}
<div class="foot">{foot}</div>
<svg id="s" width="{W}" height="{H}"><defs>
<marker id="m" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0L10,5L0,10z" fill="context-stroke"/></marker></defs></svg>
<script>
const E={json.dumps(spec['edges'])};
const s=document.getElementById('s');
function r(id){{const b=document.getElementById(id).getBoundingClientRect();return b;}}
for(const [a,b,kind,lab,via,at] of E){{
 const A=r(a),B=r(b);let p;
 const col={{n:'#0D47A1',r:'#C62828',t:'#5E35B1',i:'#0277BD',o:'#EF6C00',a:'#7B1FA2'}}[kind]||'#0D47A1';
 const dash=(kind=='t'||kind=='i')?'6 4':'';
 if(via=='down'){{const x=(Math.max(A.left,B.left)+Math.min(A.right,B.right))/2;p=[[x,A.bottom],[x,B.top]];}}
 else if(via=='up'){{const x=(Math.max(A.left,B.left)+Math.min(A.right,B.right))/2;p=[[x,A.top],[x,B.bottom]];}}
 else if(via=='vd'){{const x=A.left+A.width/2;p=[[x,A.bottom],[x,B.top+B.height/2],[B.left,B.top+B.height/2]];if(B.left<x)p[2]=[B.right,B.top+B.height/2];}}
 else if(via=='vu'){{const x=A.left+A.width/2;p=[[x,A.top],[x,B.top+B.height/2],[B.left,B.top+B.height/2]];if(B.left<x)p[2]=[B.right,B.top+B.height/2];}}
 else if(via=='hd'){{const y=A.top+A.height/2;const x=B.left+B.width/2;p=[[A.right,y],[x,y],[x,B.top]];if(B.left+B.width/2<A.left)p[0]=[A.left,y];}}
 else if(via=='hu'){{const y=A.top+A.height/2;const x=B.left+B.width/2;p=[[A.right,y],[x,y],[x,B.bottom]];if(B.left+B.width/2<A.left)p[0]=[A.left,y];}}
 else if(via=='lu'){{const ya=A.top+A.height/2,x=A.left-22;p=[[A.left,ya],[x,ya],[x,B.bottom]];}}
 else if(via&&via.startsWith('top')){{const yy=+via.slice(3);const xa=A.left+A.width/2,xb=B.left+B.width/2;p=[[xa,A.top],[xa,yy],[xb,yy],[xb,B.top]];}}
 else if(via&&via.startsWith('bot')){{const yy=+via.slice(3);const xa=A.left+A.width/2,xb=B.left+B.width/2;p=[[xa,A.bottom],[xa,yy],[xb,yy],[xb,B.bottom]];}}
 else {{const ya=A.top+A.height/2,yb=B.top+B.height/2;
   if(B.left>=A.right){{const xm=(A.right+B.left)/2;p=[[A.right,ya],[xm,ya],[xm,yb],[B.left,yb]];}}
   else {{const xm=(B.right+A.left)/2;p=[[A.left,ya],[xm,ya],[xm,yb],[B.right,yb]];}} }}
 const pl=document.createElementNS('http://www.w3.org/2000/svg','polyline');
 pl.setAttribute('points',p.map(q=>q.join(',')).join(' '));pl.setAttribute('fill','none');pl.setAttribute('stroke',col);pl.setAttribute('stroke-width','2');
 if(dash)pl.setAttribute('stroke-dasharray',dash);pl.setAttribute('marker-end','url(#m)');s.appendChild(pl);
 if(lab){{const t=document.createElementNS('http://www.w3.org/2000/svg','text');const q=p[p.length-2],q2=p[p.length-1];
  let lx=q[0]+(q2[0]>q[0]?6:4),ly=q[1]+(q2[1]<q[1]?-8:(q2[1]>q[1]?16:-5));if(at){{lx=at[0];ly=at[1];}}t.setAttribute('x',lx);t.setAttribute('y',ly);t.setAttribute('class','lbl');t.setAttribute('fill',col);t.textContent=lab;s.appendChild(t);}}
}}
</script></body></html>"""


if __name__ == "__main__":
    import importlib.util
    spec = importlib.util.spec_from_file_location("d", sys.argv[1]); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    open(sys.argv[2], "w").write(page(m.SPEC))
