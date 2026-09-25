# Screen and field alignment findings

Findings from the screenshot reviews, fixed in the screen-by-screen alignment pass (deliverables 17 and 18). Each row
is closed with the commit that fixes it.

| # | Screen | Finding | Rule | Status |
|---|---|---|---|---|
| 1 | Collections › PR Worklist (`66-collections-worklist`) | The Disposition column shows the LOV code (`PR2307_FOR_REVERSAL`, `FOR_CHECK_PICKUP`) instead of its label | Show LOV labels, never codes (BDO_UX_GUIDELINES §4) | Open |
| 2 | Disbursement Workbench (`116-disbursement-workbench`) | Counter tile labels are upper case ("FOR APPROVAL") | Title Case labels (§2) | Open |
| 3 | FRBS › Service Fee run (`124-frbs-service-fee-run`) | Timeline entries are in sentence case ("Approved - sent for payment") while the stage heading is Title Case | Title Case stage names (WorkflowPanel `titleCase`); apply it to the timeline too | Open |
| 4 | Operations menu section and Production Reconciliation screens | The section is labelled "Product Reconciliation"; the BRD, FRS and help call it "Production Reconciliation" | Screen labels follow the FRS (test plan BRD-2 finding, FR-OP-070) | Fixed |
