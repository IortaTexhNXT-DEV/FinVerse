# Test plans per BRD (client deliverable 3)

One test plan per BRD: an Excel workbook (the master the testers execute) and a Word summary (10-20 pages for
BDOI review and sign-off). Both are built from sources in this folder by `build_test_plan.py`.

| File | Content |
|---|---|
| `brdnn_cases.yaml` | Test data of BRD nn: personas, screens, data sets, scenarios, conditions and cases per FR, roles-and-access checks, FRS findings |
| `TP_BRDnn_<NAME>.md` | Word summary in the `bdoi_docx` source format; `<!-- tp:... -->` lines are filled from the YAML |
| `build_test_plan.py` | Shared builder; the YAML schema is documented at the top of the file |

Outputs go to `docs/deliverables/out/<drop folder of the BRD>/TestPlans/` (drop map `tools/deliverables/brand.py` `BRD_DROP`):
`BIBS_TestPlan_BRD-nn_<Name>_v1.0.xlsx` and `BIBS_TestPlan_BRD-nn_<Name>_Summary_v1.0.docx`.

## Build and check

```bash
# checks only: schema, coverage, message texts and codes, automation references
python docs/deliverables/src/testplans/build_test_plan.py docs/deliverables/src/testplans/brd03_cases.yaml --check
# build the workbook and the Word summary, with page previews of the summary
python docs/deliverables/src/testplans/build_test_plan.py docs/deliverables/src/testplans/brd03_cases.yaml --previews
```

The build stops, and prints each problem, when:

- an FR of the FRS has no entry, or an entry names an FR that is not in the FRS;
- an FR lacks a positive or a negative case, a condition has no case, or a BRD ID of the FRS has no case;
- a case refers to an unknown persona, scenario, data set or screen alias, or has no steps or expected result;
- (built BRDs 1-5) a quoted code does not occur in `backend/src/main` or `frontend/src`, a literal part of a quoted
  message does not occur there, or an automation reference names a test class or method that does not exist;
- (designed BRDs 6-12) a case quotes a message code or an automation reference;
- a text uses a filler word of the writing standard or names a restricted tool or vendor, or uses the retired word for seed data.

## Writing the YAML

Start from `brd03_cases.yaml` (the pilot). The FR list, titles, BRD IDs and priorities are read from the FRS, so
the YAML holds only test content.

- **Conditions** come from the FR's acceptance criteria, rules, validations and alternate flows. Write each as a
  statement that is true or false: "The preparer cannot approve the own quotation slip."
- **Cases**: one condition per case (`c:`), a short title, steps a tester can follow on the screen, and an
  expected result that says what the screen shows. Put the message in `msg: [text, CODE]`; for BRDs 6-12 use
  `msg: [text]` from the FRS and design.
- **Message text** must be the text BIBS shows (built BRDs). The FRS is not always the build: take the text from the
  `BusinessRuleException` in the code or the frontend form, and raise a finding when the FRS differs. Mark the parts
  filled in at run time with braces so the check can find the literal parts: `"Enter the rate offered by
  {Luzon Assurance Co.} (COUNTER_PROPOSAL)"`. Braces are removed in the outputs; `<placeholders>` and `'labels'`
  are also treated as run-time values.
- **Automation reference**: `TestClass#method` for backend tests, `file.test.ts#test name` for frontend tests;
  several references are separated by `; `. Give one only when the test really asserts the case.
- **Polarity**: Negative and Security-access cases count as negative, all other types as positive; set
  `neg: true` on a Boundary case whose value is refused, and `neg: false` on a Security-access case that checks
  what a role *can* see.
- **Findings**: an FRS statement that is ambiguous, untestable or differs from the build goes in `findings`; the
  cases follow the build and the finding tells the FRS owner what to change.
- Quote YAML strings that contain `: `, ` #`, commas inside flow lists `[...]` or start with a quote or bracket.

## Placeholders in the Word summary

| Placeholder | Table |
|---|---|
| `<!-- tp:counts -->` | Totals: FRs and BRD IDs covered, conditions, scenarios, cases by type and polarity, automated cases |
| `<!-- tp:coverage -->` | Coverage by FR: conditions, positive, negative, total and automated cases |
| `<!-- tp:brd-coverage -->` | Coverage by BRD ID |
| `<!-- tp:scenarios -->` | Scenarios with persona and number of cases |
| `<!-- tp:data -->` | Named data sets and their source |
| `<!-- tp:personas -->` | Personas, SIT/UAT users (built BRDs) and number of cases |
| `<!-- tp:access -->` | Roles-and-access matrix (Y / N per role) |
| `<!-- tp:automation -->` | Automated test classes referenced and how many cases each covers |
| `<!-- tp:findings -->` | FRS findings for the FRS owner |
