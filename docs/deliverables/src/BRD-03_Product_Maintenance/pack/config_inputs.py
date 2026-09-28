"""Configuration inputs of BRD-3 Product Maintenance: the entry point of the FRS ```pack blocks and of the 06 workbook.

The builder is shared by every Drop 0 set: docs/deliverables/src/signoff/config_inputs.py (the templates of this set
are in config_inputs.yaml of this folder).

Usage
  python docs/deliverables/src/BRD-03_Product_Maintenance/pack/config_inputs.py            # template workbook
  python docs/deliverables/src/BRD-03_Product_Maintenance/pack/config_inputs.py --check    # checks only
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

_NAME = "signoff_config_inputs"
if _NAME not in sys.modules:
    _spec = importlib.util.spec_from_file_location(_NAME, Path(__file__).resolve().parents[2] / "signoff" / "config_inputs.py")
    _module = importlib.util.module_from_spec(_spec)
    sys.modules[_NAME] = _module
    _spec.loader.exec_module(_module)  # type: ignore[union-attr]
shared = sys.modules[_NAME]
load, check, render = shared.load, shared.check, shared.render


def main(argv: list[str] | None = None) -> int:
    return shared.main(["BRD-03", *(sys.argv[1:] if argv is None else argv)])


if __name__ == "__main__":
    raise SystemExit(main())
