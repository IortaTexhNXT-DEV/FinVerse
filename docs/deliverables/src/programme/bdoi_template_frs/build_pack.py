"""The standard documents of a BRD pack that are written from the sources: Developer Specification (DSP), Data
Dictionary (DDI), User Stories (USR) and the "How to read this pack" note (README).

    python docs/deliverables/src/programme/bdoi_template_frs/build_pack.py --brd BRD-11 [--only DSP,DDI,USR,README]
        [--no-pdf]

The content of a BRD is in its module (CONTENT below; BRD-11: brd11_pack.py). The writers are shared: pack_dsp.py,
pack_ddi.py, pack_usr.py, pack_readme.py, on pack_common.py. The DSP and DDI read the services, screens and schema
with tools/deliverables/api_facts.py and schema_facts.py (a local PostgreSQL server is needed for the schema).

The FRS, BPS, UXD and RNO of BRD-11 are written by build_brd11_v15.py, the ICD by build_brd11_interface_spec.py and
the review workbooks by build_brd11_v15_workbooks.py.
"""

from __future__ import annotations

import argparse
import importlib
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parents[4] / "tools" / "deliverables"))
import pack_common as pc  # noqa: E402

CONTENT = {"BRD-11": "brd11_pack", "BRD-03": "brd03_pack", "BRD-06": "brd06_pack"}
FIX_VERSION = {"BRD-11": "Drop 0", "BRD-03": "Drop 0", "BRD-06": "Drop 1"}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--brd", default="BRD-11", choices=sorted(CONTENT))
    ap.add_argument("--only", default="DSP,DDI,USR,README")
    ap.add_argument("--no-pdf", action="store_true")
    args = ap.parse_args()
    K = importlib.import_module(CONTENT[args.brd])
    problems = K.check()
    if problems:
        raise SystemExit("content: " + "; ".join(problems))
    out = pc.OUT_BDOI / K.FOLDER
    with tempfile.TemporaryDirectory() as t:
        tmp = Path(t)
        for key in args.only.split(","):
            if key == "DSP":
                import pack_dsp  # noqa: PLC0415
                target, n, facts = pack_dsp.write(K, out, tmp, pdf=not args.no_pdf)
                print(f"wrote {target.name} ({n} TOC lines; {facts})")
            elif key == "DDI":
                import pack_ddi  # noqa: PLC0415
                target, counts = pack_ddi.write(K, out)
                print(f"wrote {target.name} ({counts})")
            elif key == "USR":
                import pack_usr  # noqa: PLC0415
                target, counts, _ = pack_usr.write(K, out, FIX_VERSION[args.brd])
                print(f"wrote {target.name} ({counts})")
            elif key == "README":
                import pack_readme  # noqa: PLC0415
                target, n = pack_readme.write(K, out, tmp, pdf=not args.no_pdf)
                print(f"wrote {target.name} ({n} TOC lines)")
            else:
                raise SystemExit(f"unknown document {key}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
