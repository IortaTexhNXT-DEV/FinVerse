# Business sign-off packs

The builders of the business sign-off packs. The data of a pack lives in the source folder of its BRD,
`docs/deliverables/src/<BRD-nn_Name>/` (`brand.src_dir`): `pack/` (the YAML below), `screenshots/`, `figures/`, the FRS
and test plan sources and `START_HERE_<BRD>.md`. One pack per BRD release set: the data behind the screen specifications and business chapters of FRS v2.0, the
sign-off workbook and the screen and message cases of the test plan (deliverables README, "Release and sign-off per
BRD").

| File | Content |
|---|---|
| `signoff_pack.py` | Loads a pack, checks it against the code and the FRS, renders the FRS chapters (```pack blocks) and builds the sign-off workbook |
| `START_HERE_<BRD>.md` | 00 Start Here: guide to the set (Word source; map, reading order and steps from `guide.yaml`) |
| `build_guide_deck.py` | 01 Sign-off Pack Guide deck of a BRD (PowerPoint, speaker notes on every slide), from `pack/guide.yaml` of the BRD and the pack |
| `<BRD folder>/pack/guide.yaml` | Content of the guide deck and of Start Here: documents, reading order, steps with RACI and durations, key screens and rules, caveats with examples, entry and exit criteria, handover, governance |
| `pack/pack.yaml` | Metadata, personas and their SIT users, sections of the BRD, the screen-flow links, common screen elements |
| `pack/screens/*.yaml` | One file per process area; one entry per screen |
| `pack/messages.yaml` | Where each message appears and the fix; the texts are read from the code |
| `pack/notifications.yaml`, `contract.yaml`, `documents.yaml`, `walkthroughs.yaml` | Notifications, cross-BRD interface contract, generated documents, end-to-end walkthroughs |
| `config_inputs.py` | 06 Configuration input templates of a Drop 0 set and the FRS chapter "Configuration inputs the business provides", from `pack/config_inputs.yaml` of the BRD (`python docs/deliverables/src/signoff/config_inputs.py BRD-11`); BRD-03 keeps its own copy in its pack |
| `screenshots/` of the BRD folder | PNG screenshots and document pages captured with seed data (`tools/screenshots/capture_pack.cjs`, recipe `tools/screenshots/packs/brdNN.cjs`; see `tools/screenshots/README.md`) |

## Build and check

```bash
python docs/deliverables/src/signoff/signoff_pack.py BRD-01 --check                    # checks only
python docs/deliverables/src/signoff/signoff_pack.py BRD-01                            # sign-off workbook
python docs/deliverables/src/signoff/signoff_pack.py BRD-01 --manifest m.json          # screenshot manifest
python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-01_New_Business/FRS_BRD01_NEW_BUSINESS.md  # FRS v2.0
python docs/deliverables/src/testplans/build_test_plan.py brd01_cases.yaml               # test plan v2.0
python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-01_New_Business/START_HERE_BRD01.md   # 00 Start Here
python docs/deliverables/src/signoff/build_guide_deck.py BRD-01                            # 01 guide deck (after the workbook)
python tools/deliverables/drop_index.py
```

Optional keys of `pack.yaml`: `foreign_packs` (paths of the packs of other BRDs, relative to `pack.yaml`, whose
screens a walkthrough step may show, for example My Approvals of New Business in a Product Maintenance walkthrough)
and `signatories` (`[role, organisation, signs for]` of the sign-off certificate; the New Business list by default).
For a set that signs the role matrix (BRD-11): `menu_personas` (`{ROLE: {name, user}}`, more personas whose sidebar
the FRS and the workbook show; a walkthrough step may use them), `menu_suites` (the persona menu check,
`frontend/src/navigation/personaMenus.json`: the check refuses a suite persona without a menu or with another seed
user, and notes under each menu whether the persona sees the screens of its suite sections, no more, no less) and
`menu_sheets: per_persona` (one workbook sheet per persona, with the review columns, besides Menu by persona).
Optional keys of `messages.yaml` › `sources`: `bulk_screen` (the screen named for upload row messages) and
`ui_patterns: extended` (also reads the screen checks written as conditional texts, `problems.push(...)` and the
alternatives of a template text; the packs issued before keep the default reading). The two column checks of every
upload type are listed only for a pack with upload types.

The check refuses: a field label or button that is not a text of `frontend/src` (or a workflow stage or action), a
route that is not a screen of the menu, a screen no persona can open, an FR that is not in the FRS, a test-plan screen
alias that does not exist, an error or validation message without a fix, a flow or walkthrough link to an unknown
screen, and the words the writing standard bans. A label in `[square brackets]` describes screen content (tiles, tags,
a panel) instead of quoting a screen text and is not checked; the brackets are not printed.

## Screen entry

```yaml
- id: SCR-NB-02                     # SCR-<code>-nn, numbered in process order
  title: New Client and Edit Client
  route: /crm/clients/new           # route of navigation/modules.ts; decides personas, permission and menu path
  also: [/crm/clients/:id/edit]     # other routes of the same screen (optional)
  parent: /crm/clients                  # for a screen without a menu entry: the screen it is reached from
  public: true                      # only for the screens before sign-in (no menu, no permission)
  everyone: true                    # a page or dialog of every signed-in user without a route of its own
  menu: "Dialog over any screen"    # the menu path text of a public or everyone screen
  purpose: >-
    What the screen is for.
  entry: ["Clients › New Client", "Client record › Edit"]   # other ways in (the menu path is added)
  shots:                            # screenshots; the first carries the numbered callouts of the fields
    - {state: filled, user: ao, fill: new_client, caption: New Client form with the identity entered}
    - {state: error, user: ao, fill: new_client_invalid, submit: "^save as prospect$", caption: Field messages}
                                    # keys for the capture: open (first / a reference), tab, click, fill, submit
  fields:                           # section | label | type | length / format | mandatory | source / list |
    - "Identity | Client type | Drop-down | Individual or Corporate | Y | Fixed list | Individual | New prospect | - | -"
                                    #   default | editable in | validation | message when it fails
  actions:                          # button | who | enabled when | what happens | resulting status | notification
    - "Save as prospect | CLIENT_MAINTAIN | No blocking duplicate | Saves the prospect | Prospect | -"
  rules: ["Only the client type and the name are needed to save a prospect (FR-NB-031)."]
  outcome: The prospect exists with its code.
  frs: [FR-NB-031, FR-NB-033]
  tests: [NEWCLIENT]                # screen aliases of the test plan YAML
```

Screenshots are named `<screen id>-<nn>-<state>.png` (walkthrough steps and documents by their slug) and are captured
with seed data only by `tools/screenshots/capture_pack.cjs`. Until a screenshot exists, the FRS shows a framed
placeholder with its caption.
